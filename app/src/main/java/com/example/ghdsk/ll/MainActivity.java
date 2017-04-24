package com.example.ghdsk.ll;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int REQUEST_ENABLE_BT = 10; //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);를 위해 사용
    BluetoothAdapter mBluetoothAdapter; //블루투스의 지원 여부 확인을 위한 어댑터
    BluetoothDevice mRemoteDevice; //연결할 블루투스 원격 디바이스
    BluetoothSocket mSocket = null; //통신을 위한 소켓
    SocketThread mSocketThread = null; // 데이터 송수신 스레드
    int mPairedDeviceCount = 0; //블루투스 디바이스 개수
    Set<BluetoothDevice> mDevices; //블루투스 디바이스 Set

    Button button1;
    Button button2;
    TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        button1=(Button)findViewById(R.id.button);
        button2=(Button)findViewById(R.id.button2);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        textView = (TextView)findViewById(R.id.textView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBluetooth();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button:
                mSocketThread.write(1);
                break;
            case R.id.button2:
                mSocketThread.write(2);
        }
    }

    //연결버튼 눌렀을때 발생
    void checkBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // 장치가 블루투스를 지원하지 않는 경우
            //finish(); // 어플리케이션 종료
        } else {
            // 장치가 블루투스를 지원하는 경우
            if (!mBluetoothAdapter.isEnabled()) {
                // 블루투스를 지원하지만 비활성 상태인 경우
                // 블루투스를 활성 상태로 바꾸기 위해 사용자 동의 요청
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // 블루투스를 지원하며 활성 상태인 경우
                // 페어링 된 기기 목록을 보여주고 연결할 장치를 선택
                selectDevice();
            }
        }
    }

    //콜백
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //블루투스 키겠습니까? 하고 예 또는 아니오 선택시 결과 넘어옴
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    //블루투스가 활성 상태로 변경됨
                    Toast.makeText(this, "블루투스가 켜졌습니다.", Toast.LENGTH_SHORT).show();
                    selectDevice();
                } else if (resultCode == RESULT_CANCELED) {
                    //블루투스가 비활성화 상태임
                    Toast.makeText(getApplicationContext(), "블루투스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //연결할 디바이스 선택
    void selectDevice() {
        mDevices = mBluetoothAdapter.getBondedDevices();
        mPairedDeviceCount = mDevices.size();
        if (mPairedDeviceCount == 0) {
            // 페어링 된 장치가 없는 경우
            Toast.makeText(this, "페어링된 장치가 없습니다.", Toast.LENGTH_SHORT).show();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("연결할 블루투스 장치 선택");
        // 페어링 된 블루투스 장치의 이름 목록 작성
        List<String> listItems = new ArrayList<String>();
        for (BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
        }
        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                connectToSelectedDevice(items[item].toString());
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener(){
            // 취소 버튼 클릭시 설정
            public void onClick(DialogInterface dialog, int whichButton){
                dialog.cancel();
            }
        });
        builder.setCancelable(false); // 뒤로 가기 버튼 사용 금지
        AlertDialog alert = builder.create();
        alert.show();
    }

    //페어링된 블루투스 장치를 이름으로 찾기
    BluetoothDevice getDeviceFromBondedList(String name) {
        BluetoothDevice selectedDevice = null;
        for (BluetoothDevice device : mDevices) {
            if (name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    //원격 블루투스 장치와의 연결 시도
    void connectToSelectedDevice(String selectedDeviceName) {
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try {
            // 소켓 생성
            mSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);
            // RFCOMM 채널을 통한 연결
            mSocket.connect();
            // 데이터 수신 준비
            Toast.makeText(this, "수신 가능 상태", Toast.LENGTH_SHORT).show();
            onConnected(mSocket);
        } catch (Exception e) {
            // 블루투스 연결 중 오류 발생
            Toast.makeText(this, "연결에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 데이터 송수신 스레드
    private class SocketThread extends Thread {
        private final BluetoothSocket mmSocket; // 클라이언트 소켓
        private InputStream mmInStream; // 입력 스트림
        private OutputStream mmOutStream; // 출력 스트림

        public SocketThread(BluetoothSocket socket) {
            mmSocket = socket;

            // 입력 스트림과 출력 스트림을 구한다
            try {
                mmInStream = socket.getInputStream();
                mmOutStream = socket.getOutputStream();
            } catch (IOException e) {
                showMessage("Get Stream error");
            }
        }

        // 소켓에서 수신된 데이터를 화면에 표시한다
        public void run() {
            while (true) {
                try {
                    byte[] buffer = new byte[128];
                    String readMessage;
                    int bytes;
                    // 입력 스트림에서 데이터를 읽는다
                    if(mmInStream.available() > 2){
                        bytes = mmInStream.read(buffer);
                        readMessage = new String(buffer, 0, bytes);
                        showMessage("Receive: " + readMessage);
                    }
                    //SystemClock.sleep(1);
                } catch (IOException e) {
                    showMessage("Socket disconneted");
                    break;
                }
            }
        }

        // 데이터를 소켓으로 전송한다
        public void write(String strBuf) {
            try {
                // 출력 스트림에 데이터를 저장한다
                byte[] buffer = strBuf.getBytes();
                mmOutStream.write(buffer);
                showMessage("Send: " + strBuf);
            } catch (IOException e) {
                showMessage("Socket write error");
            }
        }
        public void write(int num) {
            try {
                mmOutStream.write(num);
                showMessage("Send: " + num);
            } catch (IOException e) {
                showMessage("Socket write error");
            }
        }
    }

    // 원격 디바이스와 접속되었으면 데이터 송수신 스레드를 시작
    public void onConnected(BluetoothSocket socket) {
        showMessage("Socket connected");

        // 데이터 송수신 스레드가 생성되어 있다면 삭제한다
        if( mSocketThread != null )
            mSocketThread = null;
        // 데이터 송수신 스레드를 시작
        mSocketThread = new SocketThread(socket);
        mSocketThread.start();
    }

    // 메시지를 화면에 표시
    public void showMessage(String strMsg) {
        // 메시지 텍스트를 핸들러에 전달
        Message msg = Message.obtain(mHandler, 0, strMsg);
        mHandler.sendMessage(msg);
        Log.d("tag1", strMsg);
    }

    // 메시지 화면 출력을 위한 핸들러
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                String strMsg = (String)msg.obj;
                textView.setText(textView.getText()+"\n"+strMsg);
            }
        }
    };

    //블루투스 소켓 닫기 및 데이터 수신 쓰레드 종료
    protected void onDestroy() {
        try {
            mSocket.close();
            Toast.makeText(this, "스레드가 종료되었습니다.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
        }
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
        }

        return super.onOptionsItemSelected(item);
    }

}