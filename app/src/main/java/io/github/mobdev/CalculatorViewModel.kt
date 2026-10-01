package io.github.mobdev

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class Operation(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷");

    companion object {
        fun fromSymbol(symbol: String): Operation? = entries.find { it.symbol == symbol }
    }
}

data class CalculatorState(
    val displayText: String = "0",
    val expressionText: String = "",
    val isError: Boolean = false,
)

class CalculatorViewModel : ViewModel() {

    private val _state = MutableLiveData(CalculatorState())
    val state: LiveData<CalculatorState> = _state

    private var accumulator: Double? = null
    private var pendingOperation: Operation? = null
    private var isAwaitingNewOperand = false
    private var lastOperand: Double? = null
    private var lastOperation: Operation? = null

    val currentDisplay: String
        get() = _state.value?.displayText ?: "0"

    val currentExpression: String
        get() = _state.value?.expressionText ?: ""

    val isError: Boolean
        get() = _state.value?.isError ?: false

    fun onDigit(digit: Int) {
        require(digit in 0..9) { "Digit must be between 0 and 9" }
        if (isError) {
            onClear()
        }

        val current = currentDisplay
        val newDisplay = when {
            isAwaitingNewOperand -> {
                isAwaitingNewOperand = false
                digit.toString()
            }
            current == "0" -> digit.toString()
            current.length >= MAX_DIGITS -> current
            else -> current + digit
        }

        _state.value = _state.value?.copy(
            displayText = newDisplay,
            isError = false,
        )
    }

    fun onDoubleZero() {
        if (isError) {
            onClear()
        }
        val current = currentDisplay
        if (current == "0" && !isAwaitingNewOperand) return

        val newDisplay = when {
            isAwaitingNewOperand -> {
                isAwaitingNewOperand = false
                "0"
            }
            current.length >= MAX_DIGITS - 1 -> current
            else -> current + "00"
        }

        _state.value = _state.value?.copy(
            displayText = newDisplay,
            isError = false,
        )
    }

    fun onDecimalPoint() {
        if (isError) {
            onClear()
        }

        val current = currentDisplay
        val newDisplay = when {
            isAwaitingNewOperand -> {
                isAwaitingNewOperand = false
                "0."
            }
            current.contains(".") -> current
            else -> "$current."
        }

        _state.value = _state.value?.copy(
            displayText = newDisplay,
            isError = false,
        )
    }

    fun onToggleSign() {
        if (isError) return
        val current = currentDisplay
        if (current == "0" || current == "0.") return

        val newDisplay = if (current.startsWith("-")) {
            current.substring(1)
        } else {
            "-$current"
        }

        _state.value = _state.value?.copy(displayText = newDisplay)
    }

    fun onBackspace() {
        if (isError || isAwaitingNewOperand) return
        val current = currentDisplay
        if (current == "0") return

        val newDisplay = if (current.length <= 1 || (current.length == 2 && current.startsWith("-"))) {
            "0"
        } else {
            current.dropLast(1)
        }

        _state.value = _state.value?.copy(displayText = newDisplay)
    }

    fun onClear() {
        accumulator = null
        pendingOperation = null
        isAwaitingNewOperand = false
        lastOperand = null
        lastOperation = null
        _state.value = CalculatorState(
            displayText = "0",
            expressionText = "",
            isError = false,
        )
    }

    fun onOperation(op: Operation) {
        if (isError) return

        val currentVal = currentDisplay.toDoubleOrNull() ?: return

        if (pendingOperation != null && isAwaitingNewOperand) {
            // Operator replacement (e.g. typed + then changed mind to ×)
            pendingOperation = op
            _state.value = _state.value?.copy(
                expressionText = "${formatNumber(accumulator ?: currentVal)} ${op.symbol}",
            )
            return
        }

        if (accumulator == null) {
            accumulator = currentVal
            pendingOperation = op
            isAwaitingNewOperand = true
            _state.value = _state.value?.copy(
                expressionText = "${formatNumber(currentVal)} ${op.symbol}",
            )
        } else {
            // Chained operation (e.g. 5 + 3 + ...)
            val prevOp = pendingOperation ?: op
            val result = executeOperation(accumulator!!, currentVal, prevOp)
            if (result == null) {
                showError()
                return
            }
            accumulator = result
            pendingOperation = op
            isAwaitingNewOperand = true
            _state.value = _state.value?.copy(
                displayText = formatNumber(result),
                expressionText = "${formatNumber(result)} ${op.symbol}",
            )
        }
    }

    fun onEquals() {
        if (isError) return

        val currentVal = currentDisplay.toDoubleOrNull() ?: return

        if (pendingOperation != null) {
            val left = accumulator ?: return
            val right = currentVal
            val op = pendingOperation!!

            val result = executeOperation(left, right, op)
            if (result == null) {
                showError()
                return
            }

            lastOperand = right
            lastOperation = op
            accumulator = result
            pendingOperation = null
            isAwaitingNewOperand = true

            _state.value = _state.value?.copy(
                displayText = formatNumber(result),
                expressionText = "${formatNumber(left)} ${op.symbol} ${formatNumber(right)} =",
            )
        } else if (lastOperation != null && lastOperand != null) {
            // Repeated equals press: e.g. 5 + 2 = 7 = 9 = 11
            val left = currentVal
            val right = lastOperand!!
            val op = lastOperation!!

            val result = executeOperation(left, right, op)
            if (result == null) {
                showError()
                return
            }

            accumulator = result
            isAwaitingNewOperand = true

            _state.value = _state.value?.copy(
                displayText = formatNumber(result),
                expressionText = "${formatNumber(left)} ${op.symbol} ${formatNumber(right)} =",
            )
        }
    }

    private fun executeOperation(left: Double, right: Double, op: Operation): Double? {
        return when (op) {
            Operation.ADD -> left + right
            Operation.SUBTRACT -> left - right
            Operation.MULTIPLY -> left * right
            Operation.DIVIDE -> {
                if (right == 0.0 || right.isNaN()) {
                    null // Division by zero
                } else {
                    left / right
                }
            }
        }
    }

    private fun showError() {
        accumulator = null
        pendingOperation = null
        lastOperand = null
        lastOperation = null
        isAwaitingNewOperand = true
        _state.value = CalculatorState(
            displayText = ERROR_MESSAGE,
            expressionText = "",
            isError = true,
        )
    }

    companion object {
        const val ERROR_MESSAGE = "Ошибка"
        private const val MAX_DIGITS = 14

        fun formatNumber(value: Double): String {
            if (value.isInfinite() || value.isNaN()) return ERROR_MESSAGE

            // Check if it's an exact integer within Long limits
            if (value == value.toLong().toDouble() && Math.abs(value) < 1e12) {
                return value.toLong().toString()
            }

            // High precision BigDecimal to strip IEEE 754 float artifacts (e.g. 0.1 + 0.2 = 0.30000000000000004)
            return try {
                val bd = BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
                bd.toPlainString()
            } catch (e: Exception) {
                val df = DecimalFormat("#.########", DecimalFormatSymbols(Locale.US))
                df.format(value)
            }
        }
    }
}
