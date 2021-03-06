package com.example.mediaproject.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.mediaproject.Data.TourInfoModel;
import com.example.mediaproject.Data.UserModel;
import com.example.mediaproject.Data.UserTourListModel;
import com.example.mediaproject.Login.LoginActivityNew;
import com.example.mediaproject.R;
import com.example.mediaproject.UpdateUserInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;

public class AccountActivity extends BaseActivity implements View.OnClickListener {

    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;


    private TextView TourLost_Myheart_count;
    private TextView TourInfo_Myheart_count;
    private TextView TourLost_MyLoad_count;
    private LinearLayout Account_SelectedTourList;
    private LinearLayout Account_SelectedHeartList;
    private LinearLayout Account_MyLoadList;
    private LinearLayout Account_getHeartUser;
    private LinearLayout Account_Followers;
    private LinearLayout Account_Following;
    private ImageView AccountUserImage;
    private TextView AccountUserName;
    private Button Update_UserInfoButton;

    Button logOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentLayout(R.layout.activity_account);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        TourLost_Myheart_count = (TextView) findViewById(R.id.TourLost_Myheart_count);
        TourInfo_Myheart_count = (TextView) findViewById(R.id.TourInfo_Myheart_count);
        TourLost_MyLoad_count = (TextView) findViewById(R.id.TourLost_MyLoad_count);
        AccountUserName = (TextView) findViewById(R.id.AccountUserName);

        Account_SelectedTourList = (LinearLayout) findViewById(R.id.Account_SelectedTourList);
        Account_SelectedHeartList = (LinearLayout) findViewById(R.id.Account_SelectedHeartList);
        Account_MyLoadList = (LinearLayout) findViewById(R.id.Account_MyLoadList);
        Account_getHeartUser = (LinearLayout) findViewById(R.id.Account_getHeartUser);
        Account_Followers = (LinearLayout) findViewById(R.id.Account_Followers);
        Account_Following = (LinearLayout) findViewById(R.id.Account_Following);

        Update_UserInfoButton = (Button) findViewById(R.id.Update_UserInfo);
        AccountUserImage = (ImageView) findViewById(R.id.AccountUserImage);


        Update_UserInfoButton.setOnClickListener(this);
        Account_SelectedTourList.setOnClickListener(this);
        Account_SelectedHeartList.setOnClickListener(this);
        Account_MyLoadList.setOnClickListener(this);
        Account_getHeartUser.setOnClickListener(this);
        Account_Followers.setOnClickListener(this);
        Account_Following.setOnClickListener(this);

        Button logOut = (Button) findViewById(R.id.logout_button);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
//                LoginManager.getInstance().logOut();
                Intent intent = new Intent(AccountActivity.this, LoginActivityNew.class);
                startActivity(intent);
                finish();
            }
        });

        firebaseDatabase.getReference().child("UserTourListImage").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int UserTourListCount = 0;
                int UserLoadListCount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    UserTourListModel get = snapshot.getValue(UserTourListModel.class);

                    if (get.stars.containsKey(firebaseAuth.getCurrentUser().getUid())) {
                        UserTourListCount++;
                    }

                    if(get.Uid.equals(firebaseAuth.getCurrentUser().getUid())){
                        UserLoadListCount++;
                    }
                }
                String count = String.valueOf(UserTourListCount);
                TourLost_Myheart_count.setText(count);
                String count2 = String.valueOf(UserLoadListCount);
                TourLost_MyLoad_count.setText(count2);



            } // UserTourListImage end

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                TourLost_Myheart_count.setText("0");
            }
        });

        firebaseDatabase.getReference().child("TourInfo").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int TourInfocount = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    TourInfoModel get = snapshot.getValue(TourInfoModel.class);
                    if (get.stars.containsKey(firebaseAuth.getCurrentUser().getUid())) { //좋아요 횟수
                        TourInfocount++;
                    }
                }
                String count = String.valueOf(TourInfocount);
                TourInfo_Myheart_count.setText(count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                TourInfo_Myheart_count.setText("0");
            }
        });

        firebaseDatabase.getReference().child("UserInfo").child(firebaseAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserModel get = dataSnapshot.getValue(UserModel.class);

                Glide.with(AccountActivity.this)
                        .load(get.UserImage)
                        .apply(new RequestOptions().override(150, 150))
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(15)))
                        .into(AccountUserImage);

                AccountUserName.setText(get.UserDisplayName);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }//Oncreate end

    @Override
    protected void onResume() {
        super.onResume();
        setSelected(R.id.navigation_menu4);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.Account_SelectedTourList) {
            Intent intent = new Intent(AccountActivity.this, AccountListActivity.class);
            intent.putExtra("ListCheck", 0);
            startActivity(intent);
        } else if (v.getId() == R.id.Account_SelectedHeartList) {
            Intent intent = new Intent(AccountActivity.this, AccountListActivity.class);
            intent.putExtra("ListCheck", 1);
            startActivity(intent);
        } else if (v.getId() == R.id.Account_MyLoadList) {
            Intent intent = new Intent(AccountActivity.this, AccountListActivity.class);
            intent.putExtra("ListCheck", 2);
            startActivity(intent);
        }else if (v.getId() == R.id.Account_Followers) {
            Intent intent = new Intent(AccountActivity.this, AccountListActivity.class);
            intent.putExtra("ListCheck", 3);
            startActivity(intent);
        }else if (v.getId() == R.id.Account_Following) {
            Intent intent = new Intent(AccountActivity.this, AccountListActivity.class);
            intent.putExtra("ListCheck", 4);
            startActivity(intent);
        }else if (v.getId() == R.id.Account_getHeartUser) {
            Intent intent = new Intent(AccountActivity.this, AccountListActivity.class);
            intent.putExtra("ListCheck", 5);
            startActivity(intent);
        }else if (v.getId() == R.id.Update_UserInfo) {
            Intent intent = new Intent(AccountActivity.this, UpdateUserInfo.class);
            startActivity(intent);
        }

    }//onclick end
}//AccountActivity end
