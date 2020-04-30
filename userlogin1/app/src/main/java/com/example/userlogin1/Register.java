package com.example.userlogin1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Register extends AppCompatActivity {
EditText rName,rEmail,rPassword;
Button rSignUp;
TextView rLogin;
FirebaseAuth fAuth;
ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //intializing instances
        rName = findViewById(R.id.registerName);
        rEmail = findViewById(R.id.registerEmail);
        rPassword = findViewById(R.id.registerPassword);
        rSignUp = findViewById(R.id.SignUp);
        rLogin = findViewById(R.id.textLogin);
        progressBar = findViewById(R.id.registerprogressBar);

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
                String email = rEmail.getText().toString().trim();
                String password = rPassword.getText().toString().trim();

                //first need to check if user has entered null value
                if(TextUtils.isEmpty(email)){
                    rEmail.setError("Email is required");
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    rPassword.setError("Password is required");
                    return;
                }

                //Check password length more than 6
                if(password.length()<6){
                    rPassword.setError("more than 6 characters required!");
                    return;
                }

                //now All CLear, set the Progress Bar to spin
                progressBar.setVisibility(View.VISIBLE);

                //create user
                fAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
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
