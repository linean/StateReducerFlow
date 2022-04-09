# StateReducerFlow

## :scroll: Description

This repository contains an article describing my attempt to implement a simple state reducer based on [Kotlin Flow](https://kotlinlang.org/docs/flow.html) and an example app that uses it.

## :bulb: Motivation and Context

Like any Android developer following the latest trends, I like MVI architecture and the unidirectional
data flow concept. It solves many issues out of the box making our code even more bulletproof.

![Alt Text](https://media4.giphy.com/media/ZGuCTJqQxefrq/giphy.gif)

In this article, I won't go into detail about what MVI is, but you can find many great write-ups about it, e.g.
- [Modern Android Architecture with MVI design pattern](https://amsterdamstandard.com/en/post/modern-android-architecture-with-mvi-design-pattern),
- [MVI beyond state reducers](https://medium.com/bumble-tech/a-modern-kotlin-based-mvi-architecture-9924e08efab1).

Playing with libraries like [MVICore](https://github.com/badoo/MVICore), [Mobius](https://github.com/spotify/mobius), or [Orbit](https://github.com/orbit-mvi/orbit-mvi) inspired me to experiment and try to implement a flow that can perform state reduction.

That's how [StateReducerFlow](https://github.com/linean/StateReducerFlow/blob/main/app/src/main/java/com/example/statereducerflow/StateReducerFlow.kt) was born. Let me explain how I've built it, how it works, and how you can use it.

## :brain: Thinking process

_Please keep in mind that the following examples are simplified._

Let's start with a simple counter. It has one state that can be changed with two events: decrement and increment.

```
sealed class Event {
    object Increment : Event()
    object Decrement : Event()
}

data class State(
    val counter: Int = 0
)

class ViewModel {
    val state = MutableStateFlow(State())

    fun handleEvent(event: Event) {
        when (event) {
            is Increment -> state.update { it.copy(counter = it.counter + 1) }
            is Decrement -> state.update { it.copy(counter = it.counter - 1) }
        }
    }
}
```

Using the above approach, we can structure our logic in the following way:
```
Event -> ViewModel -> State
```

One issue, though, is that `handleEvent` can be called from any thread.
Having unstructured state updates can lead to tricky bugs and race conditions.
Luckily, [state.update()](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/update.html) is already thread-safe, but still, any other logic can be affected.

To solve that we can introduce a channel that will allow us to process events sequentially, no matter from which thread they come.

```
class ViewModel {

    private val events = Channel<Event>()

    val state = MutableStateFlow(State())

    init {
        events.receiveAsFlow()
            .onEach(::updateState)
            .launchIn(viewModelScope)
    }

    fun handleEvent(event: Event) {
        events.trySend(event)
    }

    private fun updateState(event: Event) {
        when (event) {
            is Increment -> state.update { it.copy(counter = it.counter + 1) }
            is Decrement -> state.update { it.copy(counter = it.counter - 1) }
        }
    }
}
```

Much better. Now we process all events sequentially but state updates are still possible outside of the `updateState` method.
Ideally, state updates should be only allowed during event processing. 

To achieve that we can implement a simple reducer using [runningFold](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/running-fold.html).

```
class ViewModel {

    private val events = Channel<Event>()

    val state = events.receiveAsFlow()
        .runningFold(State(), ::reduceState)
        .stateIn(viewModelScope, Eagerly, State())

    fun handleEvent(event: Event) {
        events.trySend(event)
    }

    private fun reduceState(currentState: State, event: Event): State {
        return when (event) {
            is Increment -> currentState.copy(counter = currentState.counter + 1)
            is Decrement -> currentState.copy(counter = currentState.counter - 1)
        }
    }
}
```

Now only the `reduceState` method can perform state transformations.

![Alt Text](https://media3.giphy.com/media/3oEjHYibHwRL7mrNyo/giphy.gif)

When you look at this example ViewModel you may notice that only the `reduceState` method contains important logic.
Everything else is just boilerplate that needs to be repeated for every new ViewModel.

As we all like to stay [DRY](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself), I needed to extract the generic logic from the ViewModel.

That's how StateReducerFlow was born.

## :rocket: StateReducerFlow

I wanted [StateReducerFlow](https://github.com/linean/StateReducerFlow/blob/main/app/src/main/java/com/example/statereducerflow/StateReducerFlow.kt) to be a [StateFlow](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow) that can handle generic events. I started with this definition:
```
interface StateReducerFlow<STATE, EVENT> : StateFlow<STATE> {
    fun handleEvent(event: EVENT)
}
```

Moving forward I extracted my ViewModel logic to the new flow implementation:

```
private class StateReducerFlowImpl<STATE, EVENT>(
    initialState: STATE,
    reduceState: (STATE, EVENT) -> STATE,
    scope: CoroutineScope
) : StateReducerFlow<STATE, EVENT> {

    private val events = Channel<EVENT>()

    private val stateFlow = events
        .receiveAsFlow()
        .runningFold(initialState, reduceState)
        .stateIn(scope, Eagerly, initialState)

    override val replayCache get() = stateFlow.replayCache

    override val value get() = stateFlow.value

    override suspend fun collect(collector: FlowCollector<STATE>): Nothing {
        stateFlow.collect(collector)
    }

    override fun handleEvent(event: EVENT) {
        events.trySend(event)
    }
}
```

As you can see, the only new things are a few overrides from [StateFlow](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow).
To construct the flow you provide the initial state, the function that can reduce it, and the coroutine scope in which the state can be shared.

The last missing part is a factory function that can create our new flow. I've decided to go with ViewModel extension to access [viewModelScope](https://developer.android.com/topic/libraries/architecture/coroutines#viewmodelscope).

```
fun <STATE, EVENT> ViewModel.StateReducerFlow(
    initialState: STATE,
    reduceState: (STATE, EVENT) -> STATE,
): StateReducerFlow<STATE, EVENT> = StateReducerFlowImpl(initialState, reduceState, viewModelScope)
```

Now we can migrate our ViewModel to the new [StateReducerFlow](https://github.com/linean/StateReducerFlow/blob/main/app/src/main/java/com/example/statereducerflow/StateReducerFlow.kt).

```
class ViewModel {

    val state = StateReducerFlow(
        initialState = State(),
        reduceState = ::reduceState
    )

    private fun reduceState(currentState: State, event: Event): State {
        return when (event) {
            is Increment -> currentState.copy(counter = currentState.counter + 1)
            is Decrement -> currentState.copy(counter = currentState.counter - 1)
        }
    }
}
```

Voilà! The boilerplate is gone.

![Alt Text](https://media2.giphy.com/media/5xaOcLHoUh2pQODngZ2/giphy.gif)

Now anyone who has access to [StateReducerFlow](https://github.com/linean/StateReducerFlow/blob/main/app/src/main/java/com/example/statereducerflow/StateReducerFlow.kt) can send events to it, e.g.

```
class ExampleActivity : Activity() {

    private val viewModel = ViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.state.handleEvent(ExampleEvent)
    }
}
```

That's it! Are you interested in how it works in a real app or how it can be tested?
See my example project: https://github.com/linean/StateReducerFlow/tree/main/app/src


<img src="https://github.com/linean/StateReducerFlow/blob/main/demo.gif?raw=true" height=300/>


Stay inspired!
