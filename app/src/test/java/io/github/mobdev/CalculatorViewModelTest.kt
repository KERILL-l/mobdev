package io.github.mobdev

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CalculatorViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        viewModel = CalculatorViewModel()
    }

    @Test
    fun `test initial state is zero`() {
        assertEquals("0", viewModel.currentDisplay)
        assertEquals("", viewModel.currentExpression)
        assertFalse(viewModel.isError)
    }

    @Test
    fun `test entering single and multiple digits`() {
        viewModel.onDigit(1)
        assertEquals("1", viewModel.currentDisplay)

        viewModel.onDigit(2)
        assertEquals("12", viewModel.currentDisplay)

        viewModel.onDigit(3)
        assertEquals("123", viewModel.currentDisplay)
    }

    @Test
    fun `test leading zeroes not duplicated`() {
        viewModel.onDigit(0)
        assertEquals("0", viewModel.currentDisplay)
        viewModel.onDigit(0)
        assertEquals("0", viewModel.currentDisplay)
        viewModel.onDoubleZero()
        assertEquals("0", viewModel.currentDisplay)
    }

    @Test
    fun `test double zero button with non-zero input`() {
        viewModel.onDigit(5)
        viewModel.onDoubleZero()
        assertEquals("500", viewModel.currentDisplay)
    }

    @Test
    fun `test decimal point input`() {
        viewModel.onDigit(3)
        viewModel.onDecimalPoint()
        viewModel.onDigit(1)
        viewModel.onDigit(4)
        assertEquals("3.14", viewModel.currentDisplay)

        // Multiple decimal points should be ignored
        viewModel.onDecimalPoint()
        assertEquals("3.14", viewModel.currentDisplay)
    }

    @Test
    fun `test decimal point as first input`() {
        viewModel.onDecimalPoint()
        viewModel.onDigit(5)
        assertEquals("0.5", viewModel.currentDisplay)
    }

    @Test
    fun `test toggle sign`() {
        viewModel.onDigit(7)
        viewModel.onToggleSign()
        assertEquals("-7", viewModel.currentDisplay)

        viewModel.onToggleSign()
        assertEquals("7", viewModel.currentDisplay)

        // Toggle sign on zero does nothing
        viewModel.onClear()
        viewModel.onToggleSign()
        assertEquals("0", viewModel.currentDisplay)
    }

    @Test
    fun `test backspace`() {
        viewModel.onDigit(1)
        viewModel.onDigit(2)
        viewModel.onDigit(3)
        assertEquals("123", viewModel.currentDisplay)

        viewModel.onBackspace()
        assertEquals("12", viewModel.currentDisplay)

        viewModel.onBackspace()
        assertEquals("1", viewModel.currentDisplay)

        viewModel.onBackspace()
        assertEquals("0", viewModel.currentDisplay)
    }

    @Test
    fun `test simple addition`() {
        viewModel.onDigit(1)
        viewModel.onDigit(5)
        viewModel.onOperation(Operation.ADD)
        assertEquals("15 +", viewModel.currentExpression)

        viewModel.onDigit(7)
        viewModel.onEquals()

        assertEquals("22", viewModel.currentDisplay)
        assertEquals("15 + 7 =", viewModel.currentExpression)
        assertFalse(viewModel.isError)
    }

    @Test
    fun `test simple subtraction`() {
        viewModel.onDigit(2)
        viewModel.onDigit(0)
        viewModel.onOperation(Operation.SUBTRACT)

        viewModel.onDigit(8)
        viewModel.onEquals()

        assertEquals("12", viewModel.currentDisplay)
        assertEquals("20 − 8 =", viewModel.currentExpression)
    }

    @Test
    fun `test simple multiplication`() {
        viewModel.onDigit(6)
        viewModel.onOperation(Operation.MULTIPLY)

        viewModel.onDigit(7)
        viewModel.onEquals()

        assertEquals("42", viewModel.currentDisplay)
        assertEquals("6 × 7 =", viewModel.currentExpression)
    }

    @Test
    fun `test simple division`() {
        viewModel.onDigit(3)
        viewModel.onDigit(6)
        viewModel.onOperation(Operation.DIVIDE)

        viewModel.onDigit(4)
        viewModel.onEquals()

        assertEquals("9", viewModel.currentDisplay)
        assertEquals("36 ÷ 4 =", viewModel.currentExpression)
    }

    @Test
    fun `test floating point arithmetic formatting`() {
        viewModel.onDigit(0)
        viewModel.onDecimalPoint()
        viewModel.onDigit(1)
        viewModel.onOperation(Operation.ADD)

        viewModel.onDigit(0)
        viewModel.onDecimalPoint()
        viewModel.onDigit(2)
        viewModel.onEquals()

        assertEquals("0.3", viewModel.currentDisplay)
    }

    @Test
    fun `test division by zero shows error`() {
        viewModel.onDigit(9)
        viewModel.onOperation(Operation.DIVIDE)
        viewModel.onDigit(0)
        viewModel.onEquals()

        assertEquals(CalculatorViewModel.ERROR_MESSAGE, viewModel.currentDisplay)
        assertTrue(viewModel.isError)

        // Typing a new digit after error should reset error state
        viewModel.onDigit(5)
        assertEquals("5", viewModel.currentDisplay)
        assertFalse(viewModel.isError)
    }

    @Test
    fun `test chained operations`() {
        viewModel.onDigit(1)
        viewModel.onDigit(0)
        viewModel.onOperation(Operation.ADD)

        viewModel.onDigit(5)
        viewModel.onOperation(Operation.MULTIPLY)

        // 10 + 5 should evaluate to 15 when next operator is pressed
        assertEquals("15", viewModel.currentDisplay)
        assertEquals("15 ×", viewModel.currentExpression)

        viewModel.onDigit(2)
        viewModel.onEquals()

        assertEquals("30", viewModel.currentDisplay)
        assertEquals("15 × 2 =", viewModel.currentExpression)
    }

    @Test
    fun `test operator replacement before entering next operand`() {
        viewModel.onDigit(5)
        viewModel.onOperation(Operation.ADD)
        assertEquals("5 +", viewModel.currentExpression)

        // Change mind to multiply
        viewModel.onOperation(Operation.MULTIPLY)
        assertEquals("5 ×", viewModel.currentExpression)

        viewModel.onDigit(4)
        viewModel.onEquals()
        assertEquals("20", viewModel.currentDisplay)
    }

    @Test
    fun `test repeated equals press`() {
        viewModel.onDigit(5)
        viewModel.onOperation(Operation.ADD)
        viewModel.onDigit(3)
        viewModel.onEquals()
        assertEquals("8", viewModel.currentDisplay)

        // Press equals again -> should add 3 again -> 11
        viewModel.onEquals()
        assertEquals("11", viewModel.currentDisplay)

        // Press equals again -> should add 3 again -> 14
        viewModel.onEquals()
        assertEquals("14", viewModel.currentDisplay)
    }

    @Test
    fun `test typing new number after equals starts fresh`() {
        viewModel.onDigit(4)
        viewModel.onOperation(Operation.ADD)
        viewModel.onDigit(6)
        viewModel.onEquals()
        assertEquals("10", viewModel.currentDisplay)

        // Type 9
        viewModel.onDigit(9)
        assertEquals("9", viewModel.currentDisplay)
    }

    @Test
    fun `test clear button resets all state`() {
        viewModel.onDigit(7)
        viewModel.onOperation(Operation.MULTIPLY)
        viewModel.onDigit(8)
        viewModel.onClear()

        assertEquals("0", viewModel.currentDisplay)
        assertEquals("", viewModel.currentExpression)
        assertFalse(viewModel.isError)
    }
}
