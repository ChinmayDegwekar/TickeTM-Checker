package com.example.chinmay.ticketm_checker;

import android.content.Intent;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.vision.barcode.Barcode;
import com.example.chinmay.ticketm_checker.barcode.BarcodeCaptureActivity;


import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 1;


    private static final int BARCODE_DATA_LENGTH = 6;
    //BARCODE DATA
    private String service;
    private String account_no;
    private String amount;
    private String validity;
    private String txnID;
    private String timeStamp;


    private TextView mResultTextView;
    private TextView isValidTextView;
    private ImageView isValidImageView;
    private TextView tvTicketType;
    private TextView tvTransactionTime;
    private TextView tvValidFor;



    public static boolean isvalid=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultTextView = (TextView) findViewById(R.id.result_textview);
        tvTicketType = (TextView) findViewById(R.id.tvTicketType);
        tvTransactionTime = (TextView) findViewById(R.id.tvTransactionTime);
        tvValidFor = (TextView) findViewById(R.id.tvValidFor);


        Button scanBarcodeButton = (Button) findViewById(R.id.scan_barcode_button);
        scanBarcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Point[] p = barcode.cornerPoints;
                    //mResultTextView.setText(barcode.displayValue);
                    mResultTextView.setText("");
                    String decode = barcode.displayValue;
                    String[] parser = decode.split("&&");

                    if(parser.length== BARCODE_DATA_LENGTH)
                    {
                        service = parser[0];            tvTicketType.setText("Service Provider  :"+service);
                        account_no = parser[1];
                        amount = parser[2];
                        validity = parser[3];
                        txnID = parser[4];
                        timeStamp = parser[5];     tvTransactionTime.setText("Trans time        :"+timeStamp);

                        Database_checker();
//                        isValidTextView = (TextView) findViewById(R.id.tvIsValid);
//
//                        isValidTextView.setVisibility(View.VISIBLE);
//
//                        if(isvalid)
//                        isValidTextView.setText("This ticket is VALID");
//                        else
//                        isValidTextView.setText("This Ticket is NOT VALID");

                    }



                } else mResultTextView.setText(R.string.no_barcode_captured);
            } else Log.e(LOG_TAG, String.format(getString(R.string.barcode_error_format),
                    CommonStatusCodes.getStatusCodeString(resultCode)));
        } else super.onActivityResult(requestCode, resultCode, data);
    }

    public void Database_checker()
    {

        // boolean isvalid = false;
        Runnable runnable = new Runnable() {
            public void run() {
                //DynamoDB calls go here
                CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        "us-west-2:6a90e3bc-32d9-4eb3-83c1-0d19aa5906fa", // Identity Pool ID
                        Regions.US_WEST_2 // Region
                );


                AmazonDynamoDBClient ddbClient = Region.getRegion(Regions.US_WEST_2) // CRUCIAL

                        .createClient(
                                AmazonDynamoDBClient.class,
                                credentialsProvider,
                                new ClientConfiguration()
                        );

                DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                TicketDetailsDb tkt = new TicketDetailsDb();

                final TicketDetailsDb selectedTicket = mapper.load(TicketDetailsDb.class,txnID);


                Log.e("rowid: ",selectedTicket.getPenalty()+" "+selectedTicket.getValidity()+" "+selectedTicket.getTime_stamp());
                //Toast.makeText(MainActivity.this,selectedTicket.getTrans_id(),Toast.LENGTH_SHORT).show();
                Log.e("current: ",getCurrentSystemTime()+"   diff: "+ elapsedTimeInMinutes(getCurrentSystemTime(),selectedTicket.getTime_stamp()));

                isValidTextView = (TextView) findViewById(R.id.tvIsValid);
                isValidImageView = (ImageView)findViewById(R.id.ivStatus);

                if(isValid(selectedTicket))
                {

                    isvalid=true;
                    //isValidTextView.setText("This ticket is Valid");
                    //isValidTextView.setVisibility(View.VISIBLE);
                    //isValidTextView.setTextColor();

                    isValidTextView.post(new Runnable() {
                        public void run() {

                            isValidTextView.setText("This ticket is Valid");
                            tvValidFor.setText("Valid for         :"+validFor(selectedTicket)+" mins");
                            //Setting Image
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                isValidImageView.setImageDrawable(getResources().getDrawable(R.drawable.correct, getApplicationContext().getTheme()));
                            } else {
                                isValidImageView.setImageDrawable(getResources().getDrawable(R.drawable.correct));
                            }

                        }
                    });
                }
                else
                {
                    isvalid = false;
//                    isValidTextView.setText("This ticket is NOT Valid");
//                    isValidTextView.setVisibility(View.VISIBLE);

                    isValidTextView.post(new Runnable() {
                        public void run() {

                            isValidTextView.setText("This ticket is NOT Valid");
                            tvValidFor.setText("InValid since     :"+validFor(selectedTicket)*-1+" mins");
                            //Setting Image
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                isValidImageView.setImageDrawable(getResources().getDrawable(R.drawable.incorrect, getApplicationContext().getTheme()));
                            } else {
                                isValidImageView.setImageDrawable(getResources().getDrawable(R.drawable.incorrect));
                            }
                        }
                    });

                }


            }
        };


        Thread mythread = new Thread(runnable);
        mythread.start();


    }

    public static String getCurrentSystemTime() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

    public static long elapsedTimeInMinutes(String current, String paymentTimeStamp)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        long diff=-1;
        try {
        Date date1 = format.parse(paymentTimeStamp);
        Date date2 = format.parse(current);
            diff = date2.getTime() - date1.getTime();
        } catch (ParseException e) {
            e.printStackTrace();        //EXCEPTION HANDLING FOR MALECIOUS QRCODES
        }
        return diff/(1000*60);
    }

    public static boolean isValid(TicketDetailsDb selectedTicket)
    {
        String paymentTimeStamp = selectedTicket.getTime_stamp();
        String current = getCurrentSystemTime();
        long minutes = elapsedTimeInMinutes(getCurrentSystemTime(),selectedTicket.getTime_stamp());

        int validityInMins  = selectedTicket.getValidity();
        Log.e("isValid: ",(1.0*validityInMins - 1.0*minutes)+"");
        if( (1.0*validityInMins - 1.0*minutes) > 0)
        {

            return true;
        }
        else
            return  false;


    }

    public static double validFor(TicketDetailsDb selectedTicket)
    {
        String paymentTimeStamp = selectedTicket.getTime_stamp();
        String current = getCurrentSystemTime();
        long minutes = elapsedTimeInMinutes(getCurrentSystemTime(),selectedTicket.getTime_stamp());

        int validityInMins  = selectedTicket.getValidity();
        Log.e("isValid: ",(1.0*validityInMins - 1.0*minutes)+"");
        return 1.0*validityInMins - 1.0*minutes;

    }
}
