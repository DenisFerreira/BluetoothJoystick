package com.denisferreira.bluetoothjoystick.ui.devices

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.denisferreira.bluetoothjoystick.R
import com.denisferreira.bluetoothjoystick.databinding.FragmentDeviceListBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class DeviceListFragment : BottomSheetDialogFragment() {

    private lateinit var deviceListViewModel: DeviceListViewModel
    private var _binding: FragmentDeviceListBinding? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var mPairedDevicesArrayAdapter: ArrayAdapter<String>? = null
    private var mNewDevicesArrayAdapter: ArrayAdapter<String>? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val getBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(context, "Bluetooth Connected", Toast.LENGTH_SHORT).show()
                fillDeviceList()
            }else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(context, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show()
                activity?.finish()
            }

        }

    private fun fillDeviceList() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            mPairedDevicesArrayAdapter!!.add(
                """
                ${device.name}
                ${device.address}
                """.trimIndent()
            )
        }
        binding.titlePairedDevices.visibility = View.VISIBLE
        if(pairedDevices?.isEmpty() == true) {
            val noDevices = resources.getText(R.string.none_paired).toString()
            mPairedDevicesArrayAdapter!!.add(noDevices)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        deviceListViewModel =
            ViewModelProvider(this).get(DeviceListViewModel::class.java)

        _binding = FragmentDeviceListBinding.inflate(inflater, container, false)

        // Get the local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(
                requireContext(),
                "Terminal does not support bluetooth",
                Toast.LENGTH_SHORT
            ).show()
            activity?.finish()
        }
        if (bluetoothAdapter?.isEnabled == true) {
            getBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            findNavController().popBackStack()
        }

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = ArrayAdapter(requireContext(), R.layout.device_name)
        mNewDevicesArrayAdapter = ArrayAdapter(requireContext(), R.layout.device_name)


        // Find and set up the ListView for paired devices
        val pairedListView = binding.pairedDevices
        pairedListView.adapter = mPairedDevicesArrayAdapter
        pairedListView.onItemClickListener = mDeviceClickListener

        // Find and set up the ListView for newly discovered devices
        val newDevicesListView = binding.newDevices
        newDevicesListView.adapter = mNewDevicesArrayAdapter
        newDevicesListView.onItemClickListener = mDeviceClickListener

        // Register for broadcasts when a device is discovered
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        requireActivity().registerReceiver(mReceiver, filter)

        _binding!!.buttonScan.setOnClickListener {
            doDiscovery()
            it.visibility = View.GONE
        }

        fillDeviceList()

        return binding.root
    }

    // The on-click listener for all devices in the ListViews
    private val mDeviceClickListener: OnItemClickListener =
        OnItemClickListener { _, v, _, _ -> // Cancel discovery because it's costly and we're about to connect
            bluetoothAdapter?.cancelDiscovery()

            // Get the device MAC address, which is the last 17 chars in the View
            val info = (v as TextView).text.toString()
            val address = info.substring(info.length - 17)

            findNavController().run {
                previousBackStackEntry?.savedStateHandle?.set(
                    EXTRA_DEVICE_ADDRESS,
                    address
                )
                popBackStack()
            }
        }


    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        bluetoothAdapter?.cancelDiscovery()

        // Unregister broadcast listeners
        requireActivity().unregisterReceiver(mReceiver)
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {

        // Indicate scanning in the title
        requireActivity().setProgressBarIndeterminateVisibility(true)
        requireActivity().setTitle(R.string.scanning)

        // Turn on sub-title for new devices
        binding.titleNewDevices.visibility = View.VISIBLE

        // If we're already discovering, stop it
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }

        // Request discover from BluetoothAdapter
        bluetoothAdapter?.startDiscovery()
    }


    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device!!.bondState != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter!!.add(
                        """
                        ${device.name}
                        ${device.address}
                        """.trimIndent()
                    )
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                requireActivity().setProgressBarIndeterminateVisibility(false)
                requireActivity().setTitle(R.string.select_device)
                if (mNewDevicesArrayAdapter!!.count == 0) {
                    val noDevices = resources.getText(R.string.none_found).toString()
                    mNewDevicesArrayAdapter!!.add(noDevices)
                }
            }
        }
    }

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE"
    }
}