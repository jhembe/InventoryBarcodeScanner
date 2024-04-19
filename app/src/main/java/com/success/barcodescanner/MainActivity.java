package com.success.barcodescanner;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnScan = findViewById(R.id.buttonScan);

        btnScan.setOnClickListener(v-> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Volume up to Flash on");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            options.setCaptureActivity(CaptureAct.class);
            barLauncher.launch(options);
        });

    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(),result->{
       if(result.getContents() != null){
           AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
           builder.setTitle("Result");
           String barcodeValue = result.getContents();
           builder.setMessage(result.getContents());
           builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                   db = FirebaseFirestore.getInstance();

                   // find the document corresponding to the scanned barcode
                   db.collection("barcodes")
                           .whereEqualTo("barcode",barcodeValue)
                           .limit(1).get().addOnCompleteListener(task -> {
                               if(task.isSuccessful()){
                                   QuerySnapshot queryDocumentSnapshots = task.getResult();
                                   if(queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()){
                                       DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                                       // get current unit count
                                       Long currentCount = document.getLong("unit");
                                       if(currentCount != null){
                                           // update unit count
                                           long newCount = Math.max(currentCount - 1,0);
                                            String product = document.getString("productType");
                                           //update the Firestore document with the new unit count
                                           document.getReference().update("unit",newCount)
                                                   .addOnSuccessListener(aVoid -> {
                                                       //Display the toast message indicating success
                                                       Toast.makeText(MainActivity.this,product+" is removed. Remaining "+newCount,Toast.LENGTH_SHORT).show();

                                                   }).addOnFailureListener(e -> {
                                                       // handling failure
                                                       Toast.makeText(MainActivity.this,"Failed to remove item",Toast.LENGTH_SHORT).show();
                                                   });
                                       }else{
                                           Toast.makeText(MainActivity.this,"Null current count",Toast.LENGTH_SHORT).show();
                                       }
                                   }else{
                                       Toast.makeText(MainActivity.this,"Barcode not found in the database",Toast.LENGTH_SHORT).show();
                                   }
                               }else{
                                   // handling query failure
                                   Exception exception = task.getException();
                                   if(exception != null){
                                       Toast.makeText(MainActivity.this,"Query Failed to execute",Toast.LENGTH_SHORT).show();
                                   }
                               }
                           });

                   dialog.dismiss();
               }
           }).show();
       }
       else{
           Toast.makeText(getApplicationContext(), "Something's wrong", Toast.LENGTH_SHORT).show();
       }
    });
}