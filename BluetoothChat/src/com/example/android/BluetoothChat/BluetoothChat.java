/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.interfaces.DSAKeyPairGenerator;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.util.Arrays;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
	
	boolean SECURE = false;
	
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    // Name of the file with stored user data
    public static final String FILE_NAME = "user_data";
    // User name
    public static String userName = "Dick Dangle";
    // User password
    public static String userPW = "Titties";
    
    private static KeyPair keys = null;
    // User public key
    public static DSAPublicKey publicKey = null;
    // User private key
    public static DSAPrivateKey privateKey = null;
    
    public static final int KEY_LENGTH = 512;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private TextView mTitle;
    private TextView mStatus;
    private ToggleButton mLockToggle;
    private Button mAddUser;
    private Button mRemoveUser;
    //private ListView mConversationView;
    //private EditText mOutEditText;
    private Button mSendButton;
    private CheckBox mAdminCheckBox;
    private TextView mAddRemoveUserText;
    private TextView mAddRemoveKeyText;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    //private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    
    // Command Buffer for args from lock system
    byte[] cmdBuffer = new byte[512];
    int cmdIndex = 0;
    
    // Store new user add/drop key
    byte[] extUserKey = new byte[192];
    String extUserName = "";
    boolean extUserObtained = false;
    
    // Store the random number given at the start of the session
    long randNum = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        //mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        //mConversationView = (ListView) findViewById(R.id.in);
        //mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        //mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        //mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                // TextView view = (TextView) findViewById(R.id.edit_text_out);
                // String message = view.getText().toString();
            	
            	//String key = new String(publicKey);
            	String pre = "<KEY_EXCHANGE:" + userName + ":";
            	byte[] preBytes = pre.getBytes();
            	byte[] send = Arrays.copyOf(preBytes, 192 + pre.getBytes().length + 1);
            	System.arraycopy(extUserKey, 0, send, preBytes.length, extUserKey.length);
            	send[send.length - 1] = (byte)'>';

            	sendByteArrayMessage(send);
            }
        });
        
        // Initialize the status 
        mStatus = (TextView) findViewById(R.id.status);
        mStatus.setText("Status: Waiting For Connection...");
        
        // Initialize the Lock toggle button and add listener for click events
        mLockToggle = (ToggleButton) findViewById(R.id.lock_toggle);
        mLockToggle.setEnabled(false);
        mLockToggle.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		if (mLockToggle.isChecked()) {
        			SendLockCommand("LOCK");
        		} else {
        			SendLockCommand("UNLOCK");
        		}
        	}
        });
        
        mAdminCheckBox = (CheckBox) findViewById(R.id.permission_checkbox);
        
        // Initialize the AddUser and RemoveUser buttons with listeners
        mAddUser = (Button) findViewById(R.id.add_button);
        mRemoveUser = (Button) findViewById(R.id.remove_button);
        mAddUser.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		SendAddRemoveCmd("ADD");
        	}
        });
        mRemoveUser.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		SendAddRemoveCmd("REMOVE");
        	}
        });
        
        mAddRemoveUserText = (TextView) findViewById(R.id.username_add_drop);
        mAddRemoveKeyText = (TextView) findViewById(R.id.key_add_drop);
        
        // Get all the stored user information from the stored file
        /*
        try {
        	FileInputStream fis = openFileInput(FILE_NAME);
        	byte[] buffer = new byte[512];
        	int bytes = fis.read(buffer, 0, 512);
        	String userInfo = new String(buffer, 0, bytes);
        	String[] userToks = userInfo.split(":");
        	userName = userToks[0];
        	userPW = userToks[1];
        	publicKey = Integer.parseInt(userToks[2]);
        	privateKey = Integer.parseInt(userToks[3]);
        	fis.close();
        	
        } catch (Exception ex) {
        	// File was not found so generate keys
        	try {
        		FileOutputStream fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
        		GenerateKeys();
        		String user = userName + ":" + userPW + ":" + publicKey + ":" + privateKey;
        		fos.write(user.getBytes(), 0, user.getBytes().length);
        		fos.close();
        		
        	} catch (Exception ex) {
        		// Should ot reach this point b/c it creates file if not found
        	}
        }
        */
        
        GenerateKeys();

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    
    private void SendAddRemoveCmd(String cmd) {
		//String name = mAddRemoveUserText.getText().toString();
		//String key = mAddRemoveKeyText.getText().toString();
		if (extUserObtained) {
			String numStr = "" + randNum;
			int size = cmd.getBytes().length +1+ userName.getBytes().length +1+ extUserName.getBytes().length +1+ extUserKey.length +1+ numStr.getBytes().length;
			if (cmd.equals("ADD"))
				size += 3;
			byte[] encrypt = new byte[size];
			byte perms = 0x01;
			if (mAdminCheckBox.isChecked())
				perms = 0x03;
			
			String pre = cmd + ":" + userName + ":" + extUserName + ":";
			System.arraycopy(pre.getBytes(), 0, encrypt, 0, pre.getBytes().length);
			System.arraycopy(extUserKey, 0, encrypt, pre.getBytes().length, extUserKey.length);
			encrypt[extUserKey.length + pre.getBytes().length] = (byte)':';
			
			int current = extUserKey.length + pre.getBytes().length + 1;
			if (cmd.equals("ADD")) {
				encrypt[current] = perms;
				encrypt[current+1] = (byte)':';
				current += 2;
			}
			System.arraycopy(numStr.getBytes(), 0, encrypt, current, numStr.getBytes().length);
			
			byte[] end = new byte[numStr.getBytes().length];
			System.arraycopy(numStr.getBytes(), 0, end, 0, numStr.getBytes().length);
			if (SECURE)
				end = HashAndEncrypt(encrypt);
			byte[] send = new byte[1 + encrypt.length + 1 + end.length + 1];
			send[0] = (byte)'<';
			System.arraycopy(encrypt, 0, send, 1, encrypt.length);
			send[encrypt.length + 1] = (byte)'>';
			System.arraycopy(end, 0, send, encrypt.length + 2, end.length);
			send[send.length - 1] = (byte)'>';
			
			sendByteArrayMessage(send);
			
			mAddRemoveUserText.setText("User Name: ");
			mAddRemoveKeyText.setText("User's Key: ");
			extUserObtained = false;
		}
    }
    
    // Generate a new Public Key for the user
    private void GenerateKeys() {
    	try {
    		DSAKeyPairGenerator gen = (DSAKeyPairGenerator)KeyPairGenerator.getInstance("DSA");
    		gen.initialize(KEY_LENGTH, false, new SecureRandom());
    		keys = ((KeyPairGenerator)gen).generateKeyPair();
    		publicKey = (DSAPublicKey)keys.getPublic();
    		privateKey = (DSAPrivateKey)keys.getPrivate();
    		
    	} catch (Exception ex) {
    		// No such algorithm exception
    	}
    }
    
    // Send the lock command to the locking system
    private void SendLockCommand(String cmd) {
    	if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED) {
    		String info = cmd + ":" + userName + ":" + randNum;
    		byte[] encryptInfo = info.getBytes();
    		byte[] encrypt = ("" + randNum).getBytes();
    		if (SECURE)
    			encrypt = HashAndEncrypt(encryptInfo);
    		String encypt = new String(encrypt);
    		String send = "<" + info + ":" + encypt + ">";
    		
    		sendByteArrayMessage(send.getBytes());
    	}
    }
    
    // Hash and encrypt the string, returning the result
    private byte[] HashAndEncrypt(byte[] str) {
    	try {
	    	// First hash the message using java's message digest class
	    	MessageDigest hash = MessageDigest.getInstance("SHA-1");
	    	hash.reset();
	    	hash.update(str);
	    	return hash.digest();
    	} catch (Exception ex) {
    		// No such algorithm exists
    	}
    	
    	// Next sign the hashed message using the private key
    	
    	// Ask David if I should sign or encrypt
    	return new byte[10];
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendByteArrayMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            mChatService.write(message);

            // Reset out string buffer to zero and clear the edit text field
            //mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendByteArrayMessage(message.getBytes());
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    mLockToggle.setEnabled(true);
                    mStatus.setText("Status: Connected");
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    mLockToggle.setEnabled(false);
                    mStatus.setText("Status: Waiting For Connection...");
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    mLockToggle.setEnabled(false);
                    mStatus.setText("Status: Waiting For Connection...");
                    break;
                }
                break;
            case MESSAGE_WRITE:
                //byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                //String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //mStatus.setText(readMessage);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                
                
                CopyBuffer(readBuf, msg.arg1);
                synchronized (cmdBuffer){
                	if (cmdBuffer[cmdIndex - 1] == (byte)('>')) {
                		//mStatus.setText("Parsing Command");
                		ParseCommand();
                	}
                }
                
                
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    public void ParseCommand() {
    	String msg = new String(cmdBuffer, 1, cmdIndex-2);
    	String[] tokens = msg.split(":");
    	
    	if (tokens[0].equals("BEGINACK")) {
    		// Save the random number for future use
    		randNum = Long.parseLong(tokens[1]);
    		
    	} else if (tokens[0].equals("STATUS")) {
    		// Update the status to the user
    		mStatus.setText("Status: " + tokens[1]);
    		if (tokens[1].equals("LOCKED")) {
    			mLockToggle.setChecked(true);
    		} else {
    			mLockToggle.setChecked(false);
    		}
    		
    	} else if (tokens[0].equals("KEY_EXCHANGE")) {
    		// Add the transferred user name and key to text boxes
    		mAddRemoveUserText.setText("User Name: " + tokens[1]);
    		mAddRemoveKeyText.setText("User's Key: Loaded");
    		extUserName = tokens[1];
    		extUserKey = Arrays.copyOfRange(cmdBuffer, cmdIndex-1-192, cmdIndex-1);
    		extUserObtained = true;
    		
    	} else {
    		// Unknown cmd
    	}
    	
    	// Reset the index of the command buffer
    	cmdIndex = 0;
    }
    
    public void CopyBuffer(byte[] readBuffer, int length) {
    	synchronized (cmdBuffer){
	    	for (int j = 0; j < length; j++) {
	    		cmdBuffer[cmdIndex + j] = readBuffer[j];
	    	}
	    	cmdIndex += length;
    	}
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

}
