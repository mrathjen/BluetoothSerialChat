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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
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
	
	private static byte[] mod = { (byte)0x00,
		(byte)0x87, (byte)0xE6, (byte)0x72, (byte)0xEA, (byte)0x38, (byte)0x82, (byte)0x3D, (byte)0x8B, 
		(byte)0xC0, (byte)0x2F, (byte)0x85, (byte)0xBD, (byte)0x1C, (byte)0xF2, (byte)0x31, (byte)0x67, 
		(byte)0x2E, (byte)0x39, (byte)0x1D, (byte)0x64, (byte)0xC4, (byte)0x53, (byte)0x51, (byte)0x43, 
		(byte)0x7E, (byte)0x2E, (byte)0xF0, (byte)0x42, (byte)0xA5, (byte)0xE8, (byte)0x3C, (byte)0xAA, 
		(byte)0x84, (byte)0xA9, (byte)0xC6, (byte)0x97, (byte)0xA6, (byte)0x90, (byte)0xF9, (byte)0x7A, 
		(byte)0x14, (byte)0x2E, (byte)0xCF, (byte)0xF0, (byte)0x42, (byte)0x49, (byte)0x5A, (byte)0x5F, 
		(byte)0x60, (byte)0x7E, (byte)0x6E, (byte)0xA2, (byte)0xB3, (byte)0x87, (byte)0xBC, (byte)0x9E, 
		(byte)0xBD, (byte)0x9D, (byte)0xBF, (byte)0x06, (byte)0x7F, (byte)0x9E, (byte)0x2C, (byte)0x06, 
		(byte)0x3B, (byte)0x84, (byte)0x4C, (byte)0xDF, (byte)0xEE, (byte)0x33, (byte)0x76, (byte)0x3A, 
		(byte)0xE2, (byte)0x5E, (byte)0xB3, (byte)0x58, (byte)0x3C, (byte)0x07, (byte)0x4C, (byte)0x88, 
		(byte)0x7B, (byte)0xA6, (byte)0xBB, (byte)0x2E, (byte)0xDF, (byte)0x25, (byte)0x7B, (byte)0x77, 
		(byte)0x2B, (byte)0x7D, (byte)0xF1, (byte)0xE0, (byte)0xC2, (byte)0x5D, (byte)0xAD, (byte)0xDE, 
		(byte)0x7F, (byte)0xD0, (byte)0x34, (byte)0xFD, (byte)0x4F, (byte)0xB0, (byte)0x0D, (byte)0xCA, 
		(byte)0x69, (byte)0x08, (byte)0xE9, (byte)0x03, (byte)0x0E, (byte)0x15, (byte)0x5F, (byte)0x11, 
		(byte)0xD2, (byte)0x66, (byte)0x36, (byte)0xD5, (byte)0xCE, (byte)0x70, (byte)0x17, (byte)0x8E, 
		(byte)0x01, (byte)0xF4, (byte)0x93, (byte)0x36, (byte)0x25, (byte)0xDB, (byte)0xF3, (byte)0x43 };

	private static byte[] privateExp = {
		(byte)0x0C, (byte)0x57, (byte)0x19, (byte)0xB2, (byte)0x39, (byte)0x05, (byte)0x62, (byte)0x8F, 
		(byte)0x51, (byte)0x19, (byte)0x3F, (byte)0x9C, (byte)0xA7, (byte)0x87, (byte)0x3A, (byte)0x83, 
		(byte)0x33, (byte)0x08, (byte)0x4E, (byte)0xA9, (byte)0xFA, (byte)0xC5, (byte)0xD2, (byte)0x08, 
		(byte)0x3D, (byte)0xEA, (byte)0x07, (byte)0x39, (byte)0x16, (byte)0x15, (byte)0x9B, (byte)0x84, 
		(byte)0xA4, (byte)0x5D, (byte)0x42, (byte)0x42, (byte)0x3D, (byte)0x06, (byte)0xC7, (byte)0x10, 
		(byte)0x95, (byte)0xCA, (byte)0x96, (byte)0x69, (byte)0x2B, (byte)0xAB, (byte)0xBB, (byte)0x80, 
		(byte)0x13, (byte)0xA4, (byte)0x07, (byte)0x69, (byte)0xD0, (byte)0xC1, (byte)0x8F, (byte)0x98, 
		(byte)0x1E, (byte)0x81, (byte)0xB7, (byte)0x79, (byte)0xE0, (byte)0x96, (byte)0xBD, (byte)0x4A, 
		(byte)0x85, (byte)0x9C, (byte)0xE2, (byte)0x92, (byte)0x89, (byte)0x09, (byte)0xFB, (byte)0x54, 
		(byte)0x13, (byte)0x5B, (byte)0x5F, (byte)0x3B, (byte)0x3D, (byte)0x03, (byte)0xA7, (byte)0x12, 
		(byte)0x86, (byte)0xA9, (byte)0x67, (byte)0xDB, (byte)0x2A, (byte)0x0C, (byte)0x36, (byte)0x2C, 
		(byte)0x8D, (byte)0xBB, (byte)0xA0, (byte)0xCB, (byte)0xE9, (byte)0x18, (byte)0x4C, (byte)0xC3, 
		(byte)0xC0, (byte)0xAD, (byte)0x45, (byte)0xCC, (byte)0x88, (byte)0xC8, (byte)0x76, (byte)0x78, 
		(byte)0x1C, (byte)0x58, (byte)0xAD, (byte)0xD7, (byte)0xCA, (byte)0x17, (byte)0xFB, (byte)0x82, 
		(byte)0x9A, (byte)0xC9, (byte)0xB1, (byte)0x4E, (byte)0xF9, (byte)0xC8, (byte)0x9A, (byte)0x62, 
		(byte)0x2A, (byte)0x30, (byte)0xBB, (byte)0xAF, (byte)0xCB, (byte)0xB9, (byte)0xB9, (byte)0x01 };

	private static byte[] publicExp = { (byte)0x01, (byte)0x00, (byte)0x01 };
	
	boolean SECURE = true;
	boolean GENERATE_KEYS = true;
	boolean USE_ADMIN_NAME = false;
	
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
    
    private boolean UserNameProvided = false;
    // Name of the file with stored user data
    public static final String FILE_NAME = "user_data";
    // User name
    public static String userName = "Dick Dangle";
    // User password
    public static String userPW = "Titties";
    
    private static KeyPair keys = null;
    // User public key
    public static RSAPublicKey publicKey = null;
    // User private key
    public static RSAPrivateKey privateKey = null;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    //private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private TextView mTitle;
    private TextView mStatus;
    private ToggleButton mLockToggle;
    private Button mAddUser;
    private Button mRemoveUser;
    private Button mSendButton;
    private CheckBox mAdminCheckBox;
    private TextView mAddRemoveUserText;
    private TextView mAddRemoveKeyText;
    private Button mUserButton;
    private EditText mUsernameText;
    private TextView mUsernameView;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    //private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    //private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    
    public static final int KEY_LENGTH = 1032; 		// in bits
    public static final int PUB_EXP_LENGTH = 3;		// in bytes
    public static final int PRIV_EXP_LENGTH = 1024;	// in bits
    // Command Buffer for args from lock system
    byte[] cmdBuffer = new byte[2*KEY_LENGTH];
    int cmdIndex = 0;
    
    // Store new user add/drop key
    byte[] extUserKey = new byte[KEY_LENGTH/8];
    String extUserName = "";
    boolean extUserObtained = false;
    
    private static final int RAND_NUM_LENGTH = 64;	// bytes
    // Store the random number given at the start of the session
    byte[] randNum = new byte[RAND_NUM_LENGTH];

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
        
        // Initialize the submit user button form
        mUserButton = (Button) findViewById(R.id.submit_username);
        mUsernameText = (EditText) findViewById(R.id.enter_username);
        mUsernameView = (TextView) findViewById(R.id.enter_username);
        mUserButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		SetUsername();
            }
        });
        

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	
            	String pre = "<KEY_EXCHANGE:" + userName + ":";
            	byte[] preBytes = pre.getBytes();
            	byte[] pubMod = publicKey.getModulus().toByteArray();
            	byte[] send = new byte[preBytes.length + KEY_LENGTH/8 + 1];
            	System.arraycopy(preBytes, 0, send, 0, preBytes.length);
            	System.arraycopy(pubMod, 0, send, preBytes.length, pubMod.length);
            	send[send.length - 1] = (byte)'>';

            	//byte[] test = new byte[];
            	
            	sendByteArrayMessage(send);
            }
        });
        
        // Initialize the status 
        mStatus = (TextView) findViewById(R.id.status);
        //mStatus.setText("Status: Waiting For Connection...");
        
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
        
        if (GENERATE_KEYS) {
        	
        	// Check to see if we currently have a file to read from
        	File modFile = new File("/data/data/com.example.android.BluetoothChat/files/modulous");
    	    byte[] modBytes = new byte[KEY_LENGTH/8];
    	    
    	    File pubExpFile = new File("/data/data/com.example.android.BluetoothChat/files/publicExp");
    	    byte[] pubExpBytes = new byte[PUB_EXP_LENGTH];
    	    
    	    File privExpFile = new File("/data/data/com.example.android.BluetoothChat/files/privateExp");
    	    byte[] privExpBytes = new byte[PRIV_EXP_LENGTH/8];
    	    try {
    	    	// Get the parameters from the stored files
    	        BufferedInputStream modBuf = new BufferedInputStream(new FileInputStream(modFile));
    	        modBuf.read(modBytes, 0, modBytes.length);
    	        modBuf.close();
    	        
    	        BufferedInputStream pubBuf = new BufferedInputStream(new FileInputStream(pubExpFile));
    	        pubBuf.read(pubExpBytes, 0, pubExpBytes.length);
    	        pubBuf.close();
    	        
    	        BufferedInputStream privBuf = new BufferedInputStream(new FileInputStream(privExpFile));
    	        privBuf.read(privExpBytes, 0, privExpBytes.length);
    	        privBuf.close();
    	        
		    	// Construct keys
				KeyFactory keyMaker = KeyFactory.getInstance("RSA");
		        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(new BigInteger(modBytes), new BigInteger(pubExpBytes));
		        publicKey = (RSAPublicKey)keyMaker.generatePublic(pubKeySpec);
				RSAPrivateKeySpec privKeySpec = new RSAPrivateKeySpec(new BigInteger(modBytes), new BigInteger(privExpBytes));
				privateKey = (RSAPrivateKey)keyMaker.generatePrivate(privKeySpec);
    	        
    	    } catch (FileNotFoundException e) {
    	        // this means the keys haven't been generated yet, so generate and store them in files
    	    	GenerateKeys();
    	    	try {
        		    FileOutputStream modOS = openFileOutput("modulous", Context.MODE_PRIVATE);
        		    modOS.write(privateKey.getModulus().toByteArray());
        		    modOS.close();
        		    
        		    FileOutputStream pubOS = openFileOutput("publicExp", Context.MODE_PRIVATE);
        		    pubOS.write(publicKey.getPublicExponent().toByteArray());
        		    pubOS.close();
        		    
        		    FileOutputStream privOS = openFileOutput("privateExp", Context.MODE_PRIVATE);
        		    privOS.write(privateKey.getPrivateExponent().toByteArray());
        		    privOS.close();
        		} catch (Exception e1) {
        		    e1.printStackTrace();
        		}
    	    	
    	    } catch (IOException e) {
    	        // TODO Auto-generated catch block
    	        e.printStackTrace();
    	        //mStatus.setText("Status: Bad Things");
    	    } catch (Exception ex) {
    	    	//mStatus.setText("Status: Bad Things");
    	    }
        	
        } else {
        	ConstructHardKeys();
        }

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        //mOutStringBuffer = new StringBuffer("");
        
        if (USE_ADMIN_NAME) {
        	userName = "Default Admin";
        	mUsernameView.setText("Default Admin");
        	mUsernameView.setEnabled(false);
	        mUserButton.setEnabled(false);
        } else {
	        //GetUsername();
	        try {
		    	File usernameFile = new File("/data/data/com.example.android.BluetoothChat/files/userName");
		    	int length = (int)usernameFile.length();
			    byte[] userBytes = new byte[length];
		    
		    	// Get the parameters from the stored files
		        BufferedInputStream userBuf = new BufferedInputStream(new FileInputStream(usernameFile));
		        userBuf.read(userBytes, 0, userBytes.length);
		        userBuf.close();
		        
		        userName = new String(userBytes);
		        UserNameProvided = true;
		        mUsernameView.setText(userName);
		        mUsernameView.setEnabled(false);
		        mUserButton.setEnabled(false);
		    } catch (FileNotFoundException e) {
		    	// Username has not been aquired
		    	//mStatus.setText("Status: Provide a Username!!");
		    	
		    } catch (Exception ex) {
		    	
		    }
        }
    }
    
    private void SetUsername() {
    	String newUsername = mUsernameView.getText().toString();
    	if (newUsername.length() > 0) {
	    	try {
		    	FileOutputStream userOS = openFileOutput("userName", Context.MODE_PRIVATE);
		    	userOS.write(newUsername.getBytes());
		    	userOS.close();
		    	
		    	userName = newUsername;
		    	UserNameProvided = true;
		    	mUsernameView.setEnabled(false);
		    	mUserButton.setEnabled(false);
	    	} catch (Exception ex) {
	    		
	    	}
    	}
    }
    
    private void SendAddRemoveCmd(String cmd) {
		if (extUserObtained) {
			int size = cmd.getBytes().length +1+ userName.getBytes().length +1+ extUserName.getBytes().length +1+ extUserKey.length +1+ randNum.length;
			if (cmd.equals("ADD")) size += 2;
			byte[] encrypt = new byte[size];
			byte perms = (mAdminCheckBox.isChecked()) ? ((byte)0x03) : ((byte)0x01);
			
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
			System.arraycopy(randNum, 0, encrypt, current, randNum.length);
			
			byte[] sig = HashAndEncrypt(encrypt);
			byte[] send = new byte[1 + encrypt.length + 1 + sig.length + 1];
			send[0] = (byte)'<';
			System.arraycopy(encrypt, 0, send, 1, encrypt.length);
			send[encrypt.length + 1] = (byte)':';
			System.arraycopy(sig, 0, send, encrypt.length + 2, sig.length);
			send[send.length - 1] = (byte)'>';
			
			sendByteArrayMessage(send);
			
			//mAddRemoveUserText.setText("User Name: ");
			//mAddRemoveKeyText.setText("User's Key: ");
			//extUserObtained = false;
		}
    }
    
    // Generate a new Public and Private Key for the user
    private void GenerateKeys() {
    	try {
    		//generate the RSA keys
    		KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    		gen.initialize(KEY_LENGTH, new SecureRandom());
    		keys = gen.generateKeyPair();
    		publicKey = (RSAPublicKey)keys.getPublic();
    		privateKey = (RSAPrivateKey)keys.getPrivate();
    	} catch (Exception ex) {
    		// No such algorithm exception
    	}
    }
    
    private void ConstructHardKeys() {
    	try {
	    	// Construct keys
			KeyFactory keyMaker = KeyFactory.getInstance("RSA");
	        RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(new BigInteger(mod), new BigInteger(publicExp));
	        publicKey = (RSAPublicKey)keyMaker.generatePublic(pubKeySpec);
			RSAPrivateKeySpec privKeySpec = new RSAPrivateKeySpec(new BigInteger(mod), new BigInteger(privateExp));
			privateKey = (RSAPrivateKey)keyMaker.generatePrivate(privKeySpec);
    	} catch (Exception ex) {
    		
    	}
    }
    
    // Send the lock command to the locking system
    private void SendLockCommand(String cmd) {
    	if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED) {
    		String info = cmd + ":" + userName + ":";
    		byte[] infoBytes = info.getBytes();
    		byte[] encryptInfo = new byte[infoBytes.length + RAND_NUM_LENGTH];
    		System.arraycopy(infoBytes, 0, encryptInfo, 0, infoBytes.length);
    		System.arraycopy(randNum, 0, encryptInfo, infoBytes.length, RAND_NUM_LENGTH);
    		
    		byte[] encrypt = HashAndEncrypt(encryptInfo);
    		
    		byte[] send = new byte[1+encryptInfo.length+1+encrypt.length+1];
    		send[0] = (byte)'<';
    		System.arraycopy(encryptInfo, 0, send, 1, encryptInfo.length);
    		send[1+encryptInfo.length] = (byte)':';
    		System.arraycopy(encrypt, 0, send, 1+encryptInfo.length+1, encrypt.length);
    		send[send.length-1] = (byte)'>';
    		
    		sendByteArrayMessage(send);
    	}
    }
    
    // Hash and encrypt the string, returning the result
    private byte[] HashAndEncrypt(byte[] str) {
    	try {
    		// sign the message using SHA-1 hash and RSA
    		Signature instance = Signature.getInstance("SHA1withRSA");
    		instance.initSign(privateKey);
    		instance.update(str);
    		return instance.sign();
    	} catch (Exception ex) {
    		// No such algorithm exists
    	}
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
                    GoToConnectedState();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    mLockToggle.setEnabled(false);
                    mStatus.setText("Status: Connecting to Lock");
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    GoToDisconnectState();
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
                synchronized (cmdBuffer){
                	System.arraycopy(readBuf, 0, cmdBuffer, cmdIndex, msg.arg1);
                	cmdIndex += msg.arg1;
                	
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
    
    public void GoToConnectedState() {
    	mLockToggle.setEnabled(true);
        mAddUser.setEnabled(true);
        mRemoveUser.setEnabled(true);
        mSendButton.setEnabled(true);
        mStatus.setText("Status: Connected to Lock");
    }
    
    public void GoToDisconnectState() {
    	mLockToggle.setEnabled(false);
        mStatus.setText("Status: Waiting For Connection...");
        mAddUser.setEnabled(false);
        mRemoveUser.setEnabled(false);
        mSendButton.setEnabled(false);
    }
    
    public void ParseCommand() {
    	String msg = new String(cmdBuffer, 1, cmdIndex-2);
    	String[] tokens = msg.split(":");
    	
    	if (tokens[0].equals("BEGINACK")) {
    		// Update the status to the user
    		if (tokens[1].equals("LOCKED")) {
    			mStatus.setText("Status: System is Locked");
    			mLockToggle.setChecked(true);
    		} else if (tokens[1].equals("UNLOCKED")) {
    			mStatus.setText("Status: System is Unlocked");
    			mLockToggle.setChecked(false);
    		}
    		// Save the random number for future use
    		try {
    			System.arraycopy(cmdBuffer, cmdIndex - 1 - RAND_NUM_LENGTH, randNum, 0, RAND_NUM_LENGTH);
    		} catch (Exception ex) {
    			
    		}
    	
    	} else if (tokens[0].equals("BEGIN")) {
    		
    		mLockToggle.setEnabled(false);
            mAddUser.setEnabled(false);
            mRemoveUser.setEnabled(false);
            mSendButton.setEnabled(true);
    		
    	} else if (tokens[0].equals("STATUS")) {
    		// Update the status to the user
    		if (tokens[1].equals("LOCKED")) {
    			mStatus.setText("Status: System is Locked");
    			mLockToggle.setChecked(true);
    		} else if (tokens[1].equals("LOCKED")) {
    			mStatus.setText("Status: System is Unlocked");
    			mLockToggle.setChecked(false);
    		} else if (tokens[1].equals("ADD_SUCCESS")) {
    			mStatus.setText("Status: Key Add was Successful");
    		
    		} else if (tokens[1].equals("ADD_FAILED")) {
    			mStatus.setText("Status: Key Add was Unsuccessful");
    		}
    		
    	} else if (tokens[0].equals("KEY_EXCHANGE")) {
    		// Add the transferred user name and key to text boxes
    		mAddRemoveUserText.setText("User Name: " + tokens[1]);
    		mAddRemoveKeyText.setText("User's Key: Loaded");
    		extUserName = tokens[1];
    		System.arraycopy(cmdBuffer, cmdIndex-1-(KEY_LENGTH/8), extUserKey, 0, KEY_LENGTH/8);
    		extUserObtained = true;
    		
    	} else {
    		// Unknown cmd
    	}
    	
    	// Reset the index of the command buffer
    	cmdIndex = 0;
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
            /*
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
            */
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
        case R.id.disconnect_device:
            // Launch the DeviceListActivity to see devices and do scan
            //serverIntent = new Intent(this, DeviceListActivity.class);
            //startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
        	
        	//disconnect from the current device
        	// mChatService.stop();
        	mChatService.start();
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

}
