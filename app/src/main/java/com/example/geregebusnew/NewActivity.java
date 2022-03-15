package com.example.geregebusnew;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

//import com.example.geregebusnew.helpers.Helpers;
import com.idtechproducts.device.Common;
import com.idtechproducts.device.ErrorCode;
import com.idtechproducts.device.ErrorCodeInfo;
import com.idtechproducts.device.IDTEMVData;
import com.idtechproducts.device.IDTMSRData;
import com.idtechproducts.device.IDT_VP3300;
import com.idtechproducts.device.OnReceiverListener;
import com.idtechproducts.device.OnReceiverListenerPINRequest;
import com.idtechproducts.device.ReaderInfo;
import com.idtechproducts.device.ResDataStruct;
import com.idtechproducts.device.StructConfigParameters;
import com.idtechproducts.device.audiojack.tools.FirmwareUpdateTool;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NewActivity extends AppCompatActivity implements OnReceiverListener, OnReceiverListenerPINRequest {

    ConstraintLayout mainDisplay, secondaryDisplay;
    private Boolean first_card_read, connectCard, findCard;
    private ResDataStruct resDataStruct;
    private FirmwareUpdateTool fwTool;
    private Button connectionBtn, device_search_stop;
    private int currentBalance = 0;
    private int bigThread = 0;
    private Boolean passThroughOn = false;
    private Boolean passThroughOff = false;
    private static int bigThreadCount = 10;
    IDT_VP3300 device;
    private String lastCardNumber, cardType;
    private TextView secondary_cardnumber_tv, secondary_amount_tv, secondary_balance_tv, center_text;
    private Thread t1 = null;
    private Thread t2 = null;

    private Boolean bigWhile = false;
    private int actionCode = 0 ;

    private static String currencySymbol = "₮";
    private static char decimalSeparator = '.';
    private static char groupingSeparator = ',';

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectionAction();
        msetFindView();
    }

    // findViewById
    private void msetFindView() {
        setContentView(R.layout.activity_new);
        mainDisplay = findViewById(R.id.main_display);
        secondaryDisplay = findViewById(R.id.secondary_display);
        secondaryDisplay.setVisibility(View.GONE);
        center_text = findViewById(R.id.center_text);
        secondary_amount_tv = findViewById(R.id.amountNext);
        secondary_balance_tv = findViewById(R.id.balanceNext);
        secondary_cardnumber_tv = findViewById(R.id.textViewCardNumber);
        device.setIDT_Device(fwTool);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (device.device_isConnected()) {
                    Log.d("------>", "Төхөөрөмж амжилттай холбогдсон");
                        pollOnDemandWhile();

                } else {
                    connectionAction();
                    Log.d("------>", "Холболт амжилтгүй");
                }
            }

        }, 5000); // 5000ms delay
    }

    // Холболт хийх
    void connectionAction() {

            if (device != null) {
                device.unregisterListen();
                device.release();
                device = null;
            }
            device = new IDT_VP3300(this, this, this);
            device.device_setDeviceType(ReaderInfo.DEVICE_TYPE.DEVICE_VP3300_USB);
            device.registerListen();
             device.setIDT_Device(fwTool);
    }

    // loop start
    void pollOnDemandWhile() {
        //Thread.sleep(1000);
        try {
            bigWhile = true;
            bigThread = 0;
            first_card_read = false; // Картыг эхний удаа унших нөхцөл
            while (bigWhile) {
                resDataStruct = new ResDataStruct();
                int ret = device.device_sendDataCommand("2C01", true, "01", resDataStruct);
                // passthrough mode on SUCCESS
                if (ret == ErrorCode.SUCCESS) {
                    pollOnCommand();
                    bigThread++;
                } else {
                    Log.w("TimeOut", "Time out --------------------------------->" + bigThread);
                }
            }
            checkActionCode(actionCode);
        } catch (Exception e) {

        }
    }

    private void checkActionCode(int actionCode) {
        try {

            // action code list
            // 0 Success card read & write
            // 1 disconnected card
            // 33 Банкны карт амжилттай уншсан // callback function swipeMSRData
            // 32 bank card
            // 31 bank card
            // 34 pass throuth off error
            // 35 pass throuth off success
            // 24 Бүртгэлгүй карт
            // 25 Үлдэгдэл хүрэлцэхгүй байна

            Log.i("actionCode", "actionCode -> " + actionCode);

            // Gerege карт орж ирсэн ба дэлгэц солих
            // Үлдэгдэл хүрэлцэхгүй байна
            if (actionCode == 0 || actionCode == 25) {
                changeDisplay();
            }

            // Бүртгэлгүй карт байна
            if (actionCode == 24) {
                Thread.sleep(2500);
                pollOnDemandWhile();
            }

            // Gerege карт орж ирсэн ба дэлгэц солих
            if (actionCode == 13) {
                sound("agian");
                Thread.sleep(2000);
                pollOnDemandWhile();
            }

            // Umoney card байвал 1 секундын дараа дахин асаана
            if (actionCode == 11) {
                Thread.sleep(2000);
                pollOnDemandWhile();
            }

            // Банкны карт орж ирсэн бөгөөд мэдээлэл авч чадаагүй
            if (actionCode == 34 || actionCode == 32 || actionCode == 31) {

                Boolean start = true;
                while (start) {
                    resDataStruct = new ResDataStruct();
                    int ret = device.device_sendDataCommand("2C01", true, "01", resDataStruct);

                    // passthrough mode on SUCCESS
                    if (ret == ErrorCode.SUCCESS)
                        start = false;


                }
                Thread.sleep(2000);
                pollOnDemandWhile();
            }
        } catch (Exception e) {
            Log.i("passs", "passsPollOnDemandWhile -> " + e.getStackTrace());
        }
    }

    boolean pollOnCommand() {

        // Poll on demand command
        resDataStruct = new ResDataStruct();
        int ret = device.device_sendDataCommand("2C02", true, "0090", resDataStruct);

        // Poll on demand SUCCESS
        if (ret == ErrorCode.SUCCESS) {
            reverseCardNumber(Common.bytesToHex(resDataStruct.resData));
            if ( authResult() ) { // Карт баталгаажуулалт амжилттэй ( Auth card SUCCESS )
                checkCardConnection();
            }
        } else {
            Log.w("TimeOut", "Time out --------------------------------->" + bigThread);
        }

        return true;
    }

    private void checkCardConnection() {
        try {
            connectCard = true;
            while (connectCard) {
                if ( returnBalance() == -1) { // -1 бол алдаа гарах эсвэл холболт салсан
                    connectCard = false;
                    bigWhile = false;
                } else { // Холболт салаагүй дохиолдолд дахин уншина
                    Thread.sleep(50); // 0.05 sekund
                }
            }
        } catch (Exception e) {
            Log.w("Thread-sleep", "Exception -> " + e.getStackTrace());
        }
    }

    private void writeCard(String type, int balance) {
        try {

            int amount = 0;

            if ( type.equals("01") ) amount = 200;
            else if ( type.equals("02") ) amount = 500;
            else amount = 0;

            if ( type.equals("00") ) { // Бүртгэлнүй карт
                actionCode = 24;
                data(type, actionCode, 0, 0);
            } else if (balance >= amount) {
                int nb = balance - amount;
                String dtw = reverseAmount(nb);
                resDataStruct = new ResDataStruct();

                int ret = device.device_sendDataCommand("2C08", true, dtw, resDataStruct);
                if (ret == ErrorCode.SUCCESS) {
                    actionCode = 0; // Амжилттай
                    data(type, actionCode, nb, amount);
                } else {
                    Log.e("passsWrite","2C08 FAILED Write ============>" + ret);
                }
            } else {
                actionCode = 25; // Үлдэгдэл хүрэлцэхгүй байна
                data("13", actionCode, 0, 0); // Үлдэгдэл хүрэлцэхгүй байна
            }
        } catch (Exception e) {
            Log.i("passsWriteCard","Exception " + e.getStackTrace());
        }
    }

    private void data(String type, int code, int nb, int amount) {
        sound(type);
        first_card_read = true;
        connectCard = false;
        bigWhile = false;
        actionCode = code;

        secondary_balance_tv.setText("Үлдэгдэл: " + getAmountFormatted(String.valueOf(nb)));
        secondary_amount_tv.setText("Хасагдсан дүн: " + amount + "₮");
        secondary_cardnumber_tv.setText("Картын дугаар: " + lastCardNumber.toUpperCase() + "");

        Log.d("passs","========> -" + amount +  " ");
    }

    private int returnBalance() {

        ResDataStruct resDataStruct123 = new ResDataStruct();
        int ret = device.device_sendDataCommand("2C07", true, "3310", resDataStruct123);

        if (ret == ErrorCode.SUCCESS) { // READ STATUS
            try {
                String he = Common.bytesToHex(resDataStruct123.resData);
                if ( he.length() > 10 ) {

                    String type = he.substring(he.length() / 3, he.length() / 3 + 2);
                    String totalString = he.substring(he.length() / 3 + he.length() / 3, he.length() / 3 + he.length() / 3 + 8);

                    String ts1 = totalString.substring(0, 2); //12
                    String ts2 = totalString.substring(2, 4); // 34
                    String ts3 = totalString.substring(4, 6);
                    String ts4 = totalString.substring(6, 8);

                    String total = ts4.concat(ts3).concat(ts2).concat(ts1);
                    currentBalance = Integer.parseInt(total, 16);

                    Log.d("passsSucc","Card read value --------> " + first_card_read);

                    if ( !first_card_read ) {
                        beepSound();
                        writeCard(type, currentBalance);
                    }

                } else if ( he.length() == 10 ) { // Card number || NULL || ""
                    reverseCardNumber(he);
                    currentBalance = -2;
                    Log.e("passsNumber","Card number -> " + he);
                } else {
                    currentBalance = -2;
                    Log.e("passsNumber","Card number NULL -> " + he);
                }

            } catch (Exception e) {
                currentBalance = -2;
                Log.i("passsCardRead","Exception -> " + e.getStackTrace());
            }

            return currentBalance;

        } else {

            actionCode = 13; // Disconnect card

            if ( cardType.equals("07") ) { // Банкны карт
                sound("bank");
                bankCard();
            } else if ( cardType.equals("01") ) {
                sound("uMoney");
                actionCode = 11;
            }

            Log.i("passs ","Холболт салсан байна");
            return -1;
        }
    }


    private Boolean beepSound() {
        resDataStruct = new ResDataStruct();
        int ret = device.device_sendDataCommand("0B01", true, "02", resDataStruct);
        if (ret == ErrorCode.SUCCESS) { // Auth
            return true;
        } else {
            return false;
        }
    }

    // auth
    private Boolean authResult() {
        resDataStruct = new ResDataStruct();
        int ret = device.device_sendDataCommand("2C06", true, "1001FFFFFFFFFFFF", resDataStruct);
        if (ret == ErrorCode.SUCCESS) { // Auth
            return true;
        } else {
            Log.e("passs ","Auth Error");
            return false;
        }
    }

    private void reverseCardNumber(String cardNumber){
        String he = cardNumber;
        try {
            String totalString = he.substring(2, he.length());
            String ts1 = totalString.substring(0, 2);
            String ts2 = totalString.substring(2, 4);
            String ts3 = totalString.substring(4, 6);
            String ts4 = totalString.substring(6, 8);
            cardType = he.substring(0, 2); // get card type
            lastCardNumber = ts4.concat(ts3).concat(ts2).concat(ts1);

            Log.d("reverseCardNumber", " lastCardNumber ->" + lastCardNumber + " == " + he);
        } catch (Exception e) {
            Log.e("reverseCardNumber", "Exception ------------------->" + he);
        }
    }

    private String reverseAmount(int nb) {
        String dtw = "3112", vl = "", nvl = "";

        try {
            vl += String.format("%02x", nb % 256);
            nvl += String.format("%02x", 255 - nb % 256);
            nb /= 256; // 07

            vl += String.format("%02x", nb % 256);
            nvl += String.format("%02x", 255 - nb % 256);
            nb /= 256; // 00

            vl += String.format("%02x", nb % 256);
            nvl += String.format("%02x", 255 - nb % 256);
            nb /= 256;

            vl += String.format("%02x", nb % 256);
            nvl += String.format("%02x", 255 - nb % 256);

            dtw += (vl + nvl + vl + "00000000");
        } catch (Exception e) {
            dtw = "";
            Log.e("passsWriteCard","Exception dtw" + e.getStackTrace());
        }

        return dtw;
    }

    public String getAmountFormatted(String amount) {
        try {
            return currencyFormat(String.valueOf(amount)) + " ₮";
        } catch (Exception e) {
            return amount + " ₮";
        }
    }

    public static String currencyFormat(String amount) {
        try {
            DecimalFormat decimalFormat = new DecimalFormat("#,###,###.##");
            DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
            symbols.setDecimalSeparator(decimalSeparator);
            symbols.setGroupingSeparator(groupingSeparator);
            decimalFormat.setDecimalFormatSymbols(symbols);
            String regex = "[" + currencySymbol + groupingSeparator + "]";
            String amountString = amount.replaceAll(regex, "");
            return decimalFormat.format(Double.parseDouble(amountString));
        } catch (Exception e) {
            return amount;
        }
    }

    // sounds
    private void sound(String a) {
        if ( a.equals("03") ) {
            number(R.raw.undurnastan);
        } else if ( a.equals("02") ) {
            number(R.raw.tom_hun);
        } else if ( a.equals("01") ) {
            number(R.raw.huuhed);
        } else if ( a.equals("13") ) {
            number(R.raw.uldegdel);
        }else if ( a.equals("16") ) {
            number(R.raw.unshuulsanbn);
        } else if ( a.equals("bank")) {
            number(R.raw.bank);
        } else if ( a.equals("uMoney")) {
            number(R.raw.umoney);
        } else if ( a.equals("agian")) {
            number(R.raw.dahinunsh);
        } else {
            number(R.raw.bvrtgelguicard);
        }
    }

    // Sound player
    public void number(int resourceId) {
        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(resourceId);
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            player.prepare();
            player.start();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }

            });
            player.setLooping(false);
        } catch (IOException e) {
            Log.d("aaaa", "aa");
            e.printStackTrace();
        }
    }
    /*
    private void logToScreen(String value)
    {

        EditText aa = findViewById(R.id.editTextTextMultiLine);
        aa.setText(aa.getText() + value + "\n");

    }

     */

    @Override
    public void onDestroy() {
        if (device != null) {
            device.unregisterListen();
        }

        super.onDestroy();
    }


    // ==================== change display ======================

    private void changeDisplay() {
        mainDisplay.setVisibility(View.GONE);
        secondaryDisplay.setVisibility(View.VISIBLE);
        delayFunc(2000);
    }

    private void delayFunc(int delayTime){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkCardConnection();
                actionCode = 15;
                secondaryDisplay.setVisibility(View.GONE);
                mainDisplay.setVisibility(View.VISIBLE);
                nextAction(700);
            }
        }, delayTime);
    }

    // dahin ehleh nuhtsul
    void nextAction(int delayTime) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                resDataStruct = new ResDataStruct();
                int ret = device.device_sendDataCommand("2C01", true, "00", resDataStruct);
                if (ret == ErrorCode.SUCCESS) { // Auth
                    //logToScreen("passthrough off");
                    pollOnDemandWhile();
                }
            }
        }, delayTime);
    }

    // bank card action
    private void bankCard() {

        Boolean start = true;
        while (start) {
            resDataStruct = new ResDataStruct();
            int ret = device.device_sendDataCommand("2C01", true, "00", resDataStruct);

            // passthrough mode off SUCCESS
            if (ret == ErrorCode.SUCCESS)
                start = false;
        }

        if (!start) {
            String info;
            int ret = device.msr_startMSRSwipe();
            if (ret == ErrorCode.SUCCESS) {
                info = "Please swipe/tap a card";
                actionCode = 33;
            }
            else if (ret == ErrorCode.RETURN_CODE_OK_NEXT_COMMAND)
            {
                info = "Start EMV transaction\n";
                actionCode = 32;
            }
            else {
                info = "cannot swipe/tap card\n";
                info += "Status: "+device.device_getResponseCodeString(ret)+"";
                actionCode = 31;
            }
            Log.d("infoinfoinfo"," info =>" + info);
        } else {
            actionCode = 34;
            Log.e("passTh","======>======>======>======>");
            Log.e("passTH","======>======>======>======>");
        }
    }



    // ================== CALLBACKS =========================

    IDTMSRData msr_card;
    @Override
    public void swipeMSRData(IDTMSRData card) {
        Log.d("infoinfoinfo","swipeMSRData =>" + card.result);
        Log.d("infoinfoinfo","swipeMSRData =>" + Common.bytesToHex(card.cardData));
        msr_card = card;
        String info, detail;

        if (msr_card.result != ErrorCode.SUCCESS)
        {
            info = "MSR card data didn't read correctly\n";
            info += ErrorCodeInfo.getErrorCodeDescription(msr_card.result);
            if (msr_card.result != ErrorCode.FAILED_NACK)
            {
                detail = msr_card.toString() + "  11 ";
                return;
            }
        }
        else
        {
            info = "MSR Card tapped/Swiped Successfully";
        }
        // detail = Common.parse_MSRData(device.device_getDeviceType(), msr_card);
        int len = card.cardData.length;
        String str = "";

        for(int i = 0; i < len; ++i) {
            str = str + String.format(Locale.US, "%02x ", card.cardData[i]);
        }

        secondary_balance_tv.setText("");
        secondary_amount_tv.setText("");
        secondary_cardnumber_tv.setText("Картын дугаар: " + str + "");

        Boolean start = true;
        while (start) {
            resDataStruct = new ResDataStruct();
            int ret = device.device_sendDataCommand("2C01", true, "00", resDataStruct);

            // passthrough mode on SUCCESS
            if (ret == ErrorCode.SUCCESS)
                start = false;
        }

        actionCode = 35;

        Log.d("infoinfoinfo","swipeMSRData info =>" + info);
        Log.d("infoinfoinfo","swipeMSRData detial =>" + str);

        // changeDisplay();
    }

    @Override
    public void lcdDisplay(int i, String[] strings, int i1) {
        Log.d("infoinfoinfo","lcdDisplay =>");
    }

    @Override
    public void lcdDisplay(int i, String[] strings, int i1, byte[] bytes, byte b) {
        Log.d("infoinfoinfo","lcdDisplay =>");
    }

    @Override
    public void emvTransactionData(IDTEMVData idtemvData) {
        Log.d("infoinfoinfo","emvTransactionData =>" + idtemvData.msr_cardData);
    }

    @Override
    public void deviceConnected() {
        Toast.makeText(getApplicationContext(), "Device connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void deviceDisconnected() {
        Toast.makeText(getApplicationContext(), "Device disconnected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void timeout(int i) {
        Log.d("infoinfoinfo","actionCode timeout ======> " + actionCode);
        if ( actionCode == 33 )
        {
            Boolean start = true;
            while (start) {
                resDataStruct = new ResDataStruct();
                int ret = device.device_sendDataCommand("2C01", true, "00", resDataStruct);

                // passthrough mode on SUCCESS
                if (ret == ErrorCode.SUCCESS)
                    start = false;
            }
            Log.d("infoinfoinfo","timeout =>");
            pollOnDemandWhile();
        }

    }

    @Override
    public void autoConfigCompleted(StructConfigParameters structConfigParameters) {

    }

    @Override
    public void autoConfigProgress(int i) {

    }

    @Override
    public void msgRKICompleted(String s) {

    }

    @Override
    public void ICCNotifyInfo(byte[] bytes, String s) {

    }

    @Override
    public void msgBatteryLow() {

    }

    @Override
    public void LoadXMLConfigFailureInfo(int i, String s) {

    }

    @Override
    public void msgToConnectDevice() {

    }

    @Override
    public void msgAudioVolumeAjustFailed() {

    }

    @Override
    public void dataInOutMonitor(byte[] bytes, boolean b) {

    }

    @Override
    public void pinRequest(int i, byte[] bytes, byte[] bytes1, int i1, int i2, String s) {

    }
}
