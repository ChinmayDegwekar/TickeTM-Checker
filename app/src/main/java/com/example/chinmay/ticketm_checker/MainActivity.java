package com.example.chinmay.ticketm_checker;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.example.chinmay.ticketm_checker.barcode.BarcodeCaptureActivity;


import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
import com.amazonaws.services.dynamodbv2.model.*;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultTextView = (TextView) findViewById(R.id.result_textview);

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
                    mResultTextView.setText(barcode.displayValue);
                    String decode = barcode.displayValue;
                    String[] parser = decode.split("&&");

                    if(parser.length== BARCODE_DATA_LENGTH)
                    {
                        service = parser[0];
                        account_no = parser[1];
                        amount = parser[2];
                        validity = parser[3];
                        txnID = parser[4];
                        timeStamp = parser[5];

                        Database_checker();

                    }



                } else mResultTextView.setText(R.string.no_barcode_captured);
            } else Log.e(LOG_TAG, String.format(getString(R.string.barcode_error_format),
                    CommonStatusCodes.getStatusCodeString(resultCode)));
        } else super.onActivityResult(requestCode, resultCode, data);
    }

    public void Database_checker()
    {
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

                TicketDetailsDb selectedTicket = mapper.load(TicketDetailsDb.class,"623662");
                Log.e("rowid: ",selectedTicket.getPenalty()+" "+selectedTicket.getValidity());
                Toast.makeText(MainActivity.this,selectedTicket.getTrans_id(),Toast.LENGTH_SHORT).show();

            }
        };


        Thread mythread = new Thread(runnable);
        mythread.start();


    }
}
