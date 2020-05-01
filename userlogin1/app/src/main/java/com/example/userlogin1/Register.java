package com.example.userlogin1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
EditText rName,rEmail,rPassword,rPassword2;
Button rSignUp;
TextView rLogin;
FirebaseAuth fAuth;
ProgressBar progressBar;
FirebaseFirestore fStore;
String userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //intializing instances
        rName = findViewById(R.id.registerName);
        rEmail = findViewById(R.id.registerEmail);
        rPassword = findViewById(R.id.registerPassword);
        rPassword2 = findViewById(R.id.registerPassword2);
        rSignUp = findViewById(R.id.SignUp);
        rLogin = findViewById(R.id.textLogin);
        progressBar = findViewById(R.id.registerprogressBar);
        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        //need to check if user is already signed in
        if(fAuth.getCurrentUser()!= null){
            startActivity(new Intent(getApplicationContext(),MainActivity.class));
            finish();
        }

        //when the user clicks register button need to validate the email, < 6 character password. and create user
        rSignUp.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                final String email = rEmail.getText().toString().trim();
                String password = rPassword.getText().toString().trim();
                String password2 = rPassword2.getText().toString().trim();
                final String name = rName.getText().toString();
                //first need to check if user has entered null value
                if(TextUtils.isEmpty(email)){
                    rEmail.setError("Email is required");
                    return;
                }
                if(TextUtils.isEmpty(name)){
                    rEmail.setError("Enter your Name");
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    rPassword.setError("Password is required");
                    return;
                }
                if(TextUtils.isEmpty(password2)){
                    rPassword2.setError("Confirm the password!");
                    return;
                }

                //Check password length more than 6
                if(password.length()<6){
                    rPassword.setError("more than 6 characters required!");
                    return;
                }

                if(!password.equals(password2)){
                    rPassword2.setError("Incorrect!");
                    return;
                }

                //now All CLear, set the Progress Bar to spin
                progressBar.setVisibility(View.VISIBLE);

                //create user
                fAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){

                            //adding new user details to users collection in firestore ->Collection = useres -> document identifier = userId
                            userId = fAuth.getCurrentUser().getUid();
                            DocumentReference documentReference = fStore.collection("users").document(userId);

                            Map<String,Object> user = new HashMap<>(); //key is string value is object
                            user.put("fullName",name);
                            user.put("email",email);

                            //finally adding the user details<Information> via documentReference to userId Document
                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d("DocumentReference","OnSuccess: User profile created "+ userId);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("DocumentReference","Failure: "+e.toString());
                                }
                            });

                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(Register.this,"Registered Successfully!",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        }else{
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(Register.this,"Error! "+ task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }
                }); //user created if successful
            }//onclick End
        });//buttonOnClick listner ends

        //if user wants to login
        rLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),Login.class));
            }
        });

    }//OnCreate Ends
}//Activity Ends
