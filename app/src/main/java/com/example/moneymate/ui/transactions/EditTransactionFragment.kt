package com.example.moneymate.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moneymate.databinding.FragmentEditTransactionBinding

class EditTransactionFragment : Fragment() {
    private var _binding: FragmentEditTransactionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up UI for editing
        binding.toolbarTitle.text = "Edit Transaction"
        
        // TODO: Load transaction data and populate fields
        
        binding.btnSave.setOnClickListener {
            // TODO: Save edited transaction
            findNavController().navigateUp()
        }
        
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 