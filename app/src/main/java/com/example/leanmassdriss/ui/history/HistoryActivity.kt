package com.example.leanmassdriss.ui.history

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.leanmassdriss.data.local.AppDatabase
import com.example.leanmassdriss.data.remote.FirestoreDataSource
import com.example.leanmassdriss.data.repository.LbmRepository
import com.example.leanmassdriss.databinding.ActivityHistoryBinding
import com.example.leanmassdriss.ui.calculator.LbmViewModel
import com.example.leanmassdriss.utils.SecurityUtils
import com.example.leanmassdriss.utils.collectIn
import com.example.leanmassdriss.utils.hide
import com.example.leanmassdriss.utils.show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var viewModel: LbmViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ OWASP MASVS-PLATFORM-4: Prevent screenshots and screen recording for privacy
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Senior Architect Fix: Async initialization to prevent Main Thread freeze/crash
        lifecycleScope.launch(Dispatchers.IO) {
            val passphrase   = SecurityUtils.getDatabaseKeyHex(applicationContext)
            val dao          = AppDatabase.getInstance(applicationContext, passphrase).lbmRecordDao()
            val remoteSource = FirestoreDataSource()
            val repository   = LbmRepository(dao, remoteSource)

            withContext(Dispatchers.Main) {
                val factory = LbmViewModel.Factory(repository)
                viewModel = ViewModelProvider(this@HistoryActivity, factory)[LbmViewModel::class.java]

                setupRecyclerView()
                setupListeners()
                observeHistory()
            }
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onDeleteClicked = { record ->
                viewModel.deleteRecord(record)
            }
        )
        binding.rvHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@HistoryActivity)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish() // Revient simplement à l'écran du calculateur
        }
    }

    private fun observeHistory() {
        // Collecte du flux Flow en temps réel (si suppression, la liste se met à jour instantanément)
        viewModel.allRecords.collectIn(this) { records ->
            if (records.isEmpty()) {
                binding.rvHistory.hide()
                binding.layoutEmptyState.show()
            } else {
                binding.layoutEmptyState.hide()
                binding.rvHistory.show()
                historyAdapter.submitList(records)
            }
        }
    }
}
