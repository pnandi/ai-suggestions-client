package edu.vt.cs.cs5254.dreamcatcher

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamEntryBinding
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil
import java.io.File
import java.text.DateFormat
import java.util.*

private const val TAG = "DreamDetailFragment"
private const val ARG_DREAM_ID = "dream_id"

private const val CONCEIVED_BUTTON_COLOR = "#FF059580"
private const val ENTRY_BUTTON_COLOR = "#73BF70"
private const val DEFERRED_BUTTON_COLOR = "#FF9F0505"
private const val FULFILL_BUTTON_COLOR = "#FF03A9F4"

private const val DIALOG_ADD_REFLECTION = "DialogAddReflection"
private const val REQUEST_ADD_REFLECTION = 0

class DreamDetailFragment : Fragment(), AddReflectionDialog.Callbacks {

    private var _binding: FragmentDreamDetailBinding? = null
    private val binding get() = _binding!!

    lateinit var dreamWithEntries: DreamWithEntries
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private val viewModel: DreamDetailViewModel by lazy {
        ViewModelProvider(this).get(DreamDetailViewModel::class.java)
    }

    private var adapter: DreamDetailFragment.DreamEntryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        dreamWithEntries =
            DreamWithEntries(dream = Dream(), dreamEntries = mutableListOf<DreamEntry>())

        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        Log.d(
            TAG,
            "Dream fragment created for dream with ID $dreamId"
        )
        viewModel.loadDreamWithEntries(dreamId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_detail, menu)
        val cameraAvailable = CameraUtil.isCameraAvailable(requireActivity())
        val menuItem = menu.findItem(R.id.take_dream_photo)
        menuItem.apply {
            Log.d(TAG, "Camera available: $cameraAvailable")
            isEnabled = cameraAvailable
            isVisible = cameraAvailable
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.take_dream_photo -> {
                val captureImageIntent =
                    CameraUtil.createCaptureImageIntent(requireActivity(), photoUri)
                startActivity(captureImageIntent)
                true
            }
            R.id.share_dream -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getDreamReport())
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.dream_report_subject))
                }.also { intent ->
                    val chooserIntent =
                        Intent.createChooser(intent, getString(R.string.send_report))
                    startActivity(chooserIntent)
                }
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)

        val view = binding.root
        binding.dreamEntryRecyclerView.layoutManager = LinearLayoutManager(context)

        // updateUI()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.dreamWithEntriesLiveData.observe(
            viewLifecycleOwner,
            Observer { dreamWithEntries ->
                dreamWithEntries?.let {
                    this.dreamWithEntries = dreamWithEntries
                    photoFile = viewModel.getPhotoFile(dreamWithEntries)
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                        "com.bignerdranch.android.criminalintent.fileprovider",
                        photoFile
                    )
                    updateUI()
                }
            })
        var itemTouchHelper =
            ItemTouchHelper(
                DreamEntrySwipeToDeleteCallback()
            )
        itemTouchHelper.attachToRecyclerView(binding.dreamEntryRecyclerView)

    }


    override fun onStart() {
        super.onStart()
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int, before: Int, count: Int
            ) {

                dreamWithEntries.dream.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {}
        }
        binding.dreamTitleText.addTextChangedListener(titleWatcher)

        binding.dreamFulfilledCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isFulfilled = isChecked
                if (isChecked) {
                    if (dreamWithEntries.dreamEntries.none { it.kind == DreamEntryKind.FULFILLED }) {
                        dreamWithEntries.dreamEntries += DreamEntry(
                            kind = DreamEntryKind.FULFILLED,
                            dreamId = dreamWithEntries.dream.id
                        )
                    }
                } else {
                    dreamWithEntries.dreamEntries =
                        dreamWithEntries.dreamEntries.filter { it.kind != DreamEntryKind.FULFILLED }
                }
                updateUI()
            }
        }
        binding.dreamDeferredCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isDeferred = isChecked
                if (isChecked) {
                    if (dreamWithEntries.dreamEntries.none { it.kind == DreamEntryKind.DEFERRED }) {
                        dreamWithEntries.dreamEntries += DreamEntry(
                            kind = DreamEntryKind.DEFERRED,
                            dreamId = dreamWithEntries.dream.id
                        )
                    }
                } else {
                    dreamWithEntries.dreamEntries =
                        dreamWithEntries.dreamEntries.filter { it.kind != DreamEntryKind.DEFERRED }
                }
                updateUI()
            }
        }

        binding.addReflectionButton.apply {
            setOnClickListener {
                if (!dreamWithEntries.dream.isFulfilled) {
                    AddReflectionDialog().apply {
                        setTargetFragment(this@DreamDetailFragment, REQUEST_ADD_REFLECTION)
                        show(this@DreamDetailFragment.parentFragmentManager, DIALOG_ADD_REFLECTION)
                    }
                }
            }
        }
    }

    override fun onReflectionProvided(reflectionText: String) {
        val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
        val dateString = df.format(Date()).toString()
        dreamWithEntries.dreamEntries += DreamEntry(
            text = "$dateString: $reflectionText",
            dreamId = dreamWithEntries.dream.id
        )
        updateUI()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveDreamWithEntries(dreamWithEntries)
    }

    private fun updateUI() {

        binding.dreamTitleText.setText(dreamWithEntries.dream.title)
        if (dreamWithEntries.dreamEntries.none {
                it.kind == DreamEntryKind.DEFERRED
                        || it.kind == DreamEntryKind.FULFILLED
            }) {
            binding.dreamDeferredCheckbox.isEnabled = true
            binding.dreamDeferredCheckbox.isChecked = false
            binding.dreamFulfilledCheckbox.isEnabled = true
            binding.dreamFulfilledCheckbox.isChecked = false
        }
        adapter = DreamEntryAdapter(dreamWithEntries.dreamEntries)
        binding.dreamEntryRecyclerView.adapter = adapter
        updatePhotoView()
    }

    inner class DreamEntryHolder(val itemBinding: ListItemDreamEntryBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {
        private lateinit var dreamEntry: DreamEntry

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(dreamEntry: DreamEntry) {
            this.dreamEntry = dreamEntry
            updateEntryButton(dreamEntry, itemBinding.dreamEntryButton)
        }

        override fun onClick(v: View?) {
            // no action
        }
    }

    inner class DreamEntryAdapter(var dreamEntries: List<DreamEntry>) :
        RecyclerView.Adapter<DreamDetailFragment.DreamEntryHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DreamDetailFragment.DreamEntryHolder {
            val itemBinding = ListItemDreamEntryBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return DreamEntryHolder(itemBinding)
        }

        override fun getItemCount() = dreamEntries.size
        override fun onBindViewHolder(holder: DreamEntryHolder, position: Int) {
            val dreamEntry = dreamEntries[position]
            holder.bind(dreamEntry)
        }

    }


    inner class DreamEntrySwipeToDeleteCallback :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            var pos = viewHolder.adapterPosition

            if (dreamWithEntries.dreamEntries[pos].kind == DreamEntryKind.REFLECTION) {
                dreamWithEntries.dreamEntries =
                    dreamWithEntries.dreamEntries.filter {
                        it.id != dreamWithEntries.dreamEntries[pos].id
                    }
            }

            adapter = DreamEntryAdapter(dreamWithEntries.dreamEntries)
            binding.dreamEntryRecyclerView.adapter = adapter
        }

    }

    private fun updateEntryButton(entry: DreamEntry, button: Button) {

        when (entry.kind) {
            DreamEntryKind.CONCEIVED -> {
                button.visibility = View.VISIBLE
                button.setText(R.string.dream_conceived_label)
                setButtonColor(button, CONCEIVED_BUTTON_COLOR)
            }
            DreamEntryKind.REFLECTION -> {
                button.text = entry.text
                setButtonColor(button, ENTRY_BUTTON_COLOR)
            }
            DreamEntryKind.DEFERRED -> {
                button.setText(R.string.dream_deferred_label)
                setButtonColor(button, DEFERRED_BUTTON_COLOR)
                binding.dreamDeferredCheckbox.isEnabled = true
                binding.dreamDeferredCheckbox.isChecked = true
                binding.dreamFulfilledCheckbox.isEnabled = false

            }
            DreamEntryKind.FULFILLED -> {
                button.setText(R.string.dream_fulfilled_label)
                setButtonColor(button, FULFILL_BUTTON_COLOR)
                binding.dreamFulfilledCheckbox.isEnabled = true
                binding.dreamFulfilledCheckbox.isChecked = true
                binding.dreamDeferredCheckbox.isEnabled = false
            }
        }

    }

    private fun setButtonColor(button: Button, colorString: String) {
        button.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorString))
        button.setTextColor(Color.WHITE)
        button.alpha = 1f
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = CameraUtil.getScaledBitmap(photoFile.path, requireActivity())
            binding.dreamPhoto.setImageBitmap(bitmap)
        } else {
            binding.dreamPhoto.setImageDrawable(null)
        }
    }

    private fun getDreamReport(): String {
        val newline = System.getProperty("line.separator")
        val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
        val dateString = df.format(dreamWithEntries.dream.date)
        val conceivedMsg = getString(R.string.conceived_msg) + " $dateString"
        val reflectionMsg = getString(R.string.reflection_msg)
        var statusMsg = getString(R.string.status_msg)
        var isReflection = false
        var isStatus = false

        val sb = StringBuilder()
        val reflectionSb = StringBuilder()
        sb.append(">> ${dreamWithEntries.dream.title} << $newline")

        dreamWithEntries.dreamEntries.forEach {
            when (it.kind) {
                DreamEntryKind.CONCEIVED -> {
                    sb.append("$conceivedMsg $newline")
                }
                DreamEntryKind.REFLECTION -> {
                    val reflectionText = it.text.substring(
                        it.text.indexOf(":", 0) + 2
                    )
                    isReflection = true
                    reflectionSb.append("- $reflectionText $newline")
                }
                DreamEntryKind.DEFERRED -> {
                    statusMsg += " deferred"
                    isStatus = true
                }
                DreamEntryKind.FULFILLED -> {
                    statusMsg += " fulfilled"
                    isStatus = true
                }
            }
        }
        if (isReflection) {
            sb.append("$reflectionMsg $newline$reflectionSb")
        }
        if (isStatus) {
            sb.append("$statusMsg $newline")
        }
        return sb.toString()

    }

    companion object {
        fun newInstance(dreamId: UUID): DreamDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DREAM_ID, dreamId)
            }
            return DreamDetailFragment().apply {
                arguments = args
            }
        }
    }


}