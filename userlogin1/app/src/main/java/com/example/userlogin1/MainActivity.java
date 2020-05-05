package com.example.userlogin1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
        import androidx.appcompat.app.AppCompatActivity;

        import android.content.Intent;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
        import com.google.firebase.firestore.DocumentSnapshot;
        import com.google.firebase.firestore.EventListener;
        import com.google.firebase.firestore.FirebaseFirestore;
        import com.google.firebase.firestore.FirebaseFirestoreException;

public class MainActivity extends AppCompatActivity {
TextView name,email,verifyMsg,verifyEmail;

// to get user id to access particular data from user collection in firestore
    FirebaseAuth fAuth;
//for dataRetrieve
    FirebaseFirestore fStore;
    String userId;
    FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = findViewById(R.id.mName);
        email = findViewById(R.id.mEmail);
        verifyMsg = findViewById(R.id.emailVerifiedText);
        verifyEmail = findViewById(R.id.verifyEmail);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        userId = fAuth.getCurrentUser().getUid();
        firebaseUser = fAuth.getCurrentUser();
        Log.d("User Provider is:",firebaseUser.getProviderId());



        //check email verfication
        if(firebaseUser.isEmailVerified()){
            //means email is now verified , can login
            Log.d("Verification Email:","The email "+ firebaseUser.getEmail() +"is verified");
            verifyMsg.setVisibility(View.INVISIBLE);
            verifyEmail.setVisibility(View.INVISIBLE);
        }else{
            verifyMsg.setVisibility(View.VISIBLE);
            verifyEmail.setVisibility(View.VISIBLE);
            verifyEmail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    firebaseUser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(MainActivity.this,"Verification Email has been sent!",Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("Verification Email:","Failure to send because: "+ e.getMessage());
                        }
                    });
                }
            });
        }
        //retrival of data from firestore need to make document reference
        final DocumentReference documentReference = fStore.collection("users").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                //either there will be exception(error) or documentsnapshot successfully retrieved so check exception as it can cause a crash if e exists than snapshot access will lead to crash
                if (e!=null){
                    Log.d("DocumentSnapshot","Error:"+e.getMessage());
                }else {
                    //getting the string using key defined from the document snapshot
                    name.setText(documentSnapshot.getString("fullName"));
                    email.setText(documentSnapshot.getString("email"));
                }
            }
        });

    }

    public void logout(View view){
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getApplicationContext(),Login.class));
        finish();
    }
}
