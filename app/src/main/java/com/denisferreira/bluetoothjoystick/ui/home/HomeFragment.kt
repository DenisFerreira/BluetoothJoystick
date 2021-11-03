package com.denisferreira.bluetoothjoystick.ui.home

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.denisferreira.bluetoothjoystick.R
import com.denisferreira.bluetoothjoystick.bluetooth.BluetoothRfcommClient
import com.denisferreira.bluetoothjoystick.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var mRfcommClient: BluetoothRfcommClient
    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Name of the connected device
    private var mConnectedDeviceName: String? = null

    // Local Bluetooth adapter
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private val getBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // When the request to enable Bluetooth returns
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(context, getString(R.string.bluetooth_connected), Toast.LENGTH_SHORT).show()
            }
            else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(context, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show()
                activity?.finish()
            }

        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.arcadeButton1.setColor(Color.BLUE)
        binding.arcadeButton2.setColor(Color.BLUE)

        // Get local Bluetooth adapter

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show()
            activity?.finish()

        }

        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled) {
            getBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        // Initialize the BluetoothRfcommClient to perform bluetooth connections
        mRfcommClient = BluetoothRfcommClient(context, mHandler)


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // Message types sent from the BluetoothRfcommClient Handler
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_READ = 2
        const val MESSAGE_WRITE = 3
        const val MESSAGE_DEVICE_NAME = 4
        const val MESSAGE_TOAST = 5

        // Key names received from the BluetoothRfcommClient Handler
        const val DEVICE_NAME = "device_name"
        const val TOAST = "toast"

        // Intent request codes
        private const val REQUEST_CONNECT_DEVICE = 1
        private const val REQUEST_ENABLE_BT = 2
    }

    // The Handler that gets information back from the BluetoothRfcommClient
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    BluetoothRfcommClient.STATE_CONNECTED -> {
                        binding.mTxtStatus.setText(R.string.title_connected_to)
                        binding.mTxtStatus.append(" $mConnectedDeviceName")
                    }
                    BluetoothRfcommClient.STATE_CONNECTING -> binding.mTxtStatus.setText(R.string.title_connecting)
                    BluetoothRfcommClient.STATE_NONE -> binding.mTxtStatus.setText(R.string.title_not_connected)
                }
                MESSAGE_READ -> {
                }
                MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name
                    mConnectedDeviceName =
                        msg.data.getString(DEVICE_NAME)
                    Toast.makeText(
                        context, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT
                    ).show()
                }
                MESSAGE_TOAST -> Toast.makeText(
                    context,
                    msg.data.getString(TOAST),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}