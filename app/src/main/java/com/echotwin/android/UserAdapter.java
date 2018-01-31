package com.echotwin.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

/**
 * Created by kristo.prifti on 1/22/18.
 */

public class UserAdapter extends ArrayAdapter<User> {

    UserAdapter(Context context, ArrayList<User> objects) {
        super(context, R.layout.user_row, R.id.userName, objects);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return initView(position, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return initView(position, null);
    }

    private View initView(int position, ViewGroup parent) {
        User user = getItem(position);

        View convertView = LayoutInflater.from(getContext()).inflate(R.layout.user_row, parent, false);

        TextView userName = convertView.findViewById(R.id.userName);
        ImageView userAvatar = convertView.findViewById(R.id.userAvatar);

        if (userName != null)
            userName.setText(user.getUserName());

        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://echotwin-a8d76.appspot.com");
        StorageReference pathReference = storageRef.child("Avatars/" + user.getUserAvatar());
        Glide.with(getContext())
                .using(new FirebaseImageLoader())
                .load(pathReference)
                .into(userAvatar);

        return convertView;
    }
}
