package io.github.mobdev

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.github.mobdev.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        // Digits
        binding.btn0.setOnClickListener { viewModel.onDigit(0) }
        binding.btn1.setOnClickListener { viewModel.onDigit(1) }
        binding.btn2.setOnClickListener { viewModel.onDigit(2) }
        binding.btn3.setOnClickListener { viewModel.onDigit(3) }
        binding.btn4.setOnClickListener { viewModel.onDigit(4) }
        binding.btn5.setOnClickListener { viewModel.onDigit(5) }
        binding.btn6.setOnClickListener { viewModel.onDigit(6) }
        binding.btn7.setOnClickListener { viewModel.onDigit(7) }
        binding.btn8.setOnClickListener { viewModel.onDigit(8) }
        binding.btn9.setOnClickListener { viewModel.onDigit(9) }
        binding.btn00.setOnClickListener { viewModel.onDoubleZero() }

        // Decimal point
        binding.btnDot.setOnClickListener { viewModel.onDecimalPoint() }

        // Operations
        binding.btnAdd.setOnClickListener { viewModel.onOperation(Operation.ADD) }
        binding.btnSubtract.setOnClickListener { viewModel.onOperation(Operation.SUBTRACT) }
        binding.btnMultiply.setOnClickListener { viewModel.onOperation(Operation.MULTIPLY) }
        binding.btnDivide.setOnClickListener { viewModel.onOperation(Operation.DIVIDE) }

        // Actions
        binding.btnEquals.setOnClickListener { viewModel.onEquals() }
        binding.btnC.setOnClickListener { viewModel.onClear() }
        binding.btnBackspace.setOnClickListener { viewModel.onBackspace() }
        binding.btnPlusMinus.setOnClickListener { viewModel.onToggleSign() }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            binding.tvDisplay.text = state.displayText
            binding.tvExpression.text = state.expressionText
        }
    }
}
