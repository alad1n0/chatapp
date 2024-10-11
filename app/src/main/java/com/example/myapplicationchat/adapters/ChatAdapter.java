package com.example.myapplicationchat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplicationchat.databinding.ItemContainerReceivedMessageBinding;
import com.example.myapplicationchat.databinding.ItemContainerSendMessageBinding;
import com.example.myapplicationchat.databinding.ItemContainerSendMessageImageBinding;
import com.example.myapplicationchat.databinding.ItemContainerReceivedMessageImageBinding;
import com.example.myapplicationchat.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final Bitmap receiverProfileImage;
    private final String senderId;

    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    public static final int VIEW_TYPE_IMAGE_SENT = 3;
    public static final int VIEW_TYPE_IMAGE_RECEIVED = 4;

    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new SendMessageViewHolder(
                    ItemContainerSendMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else if (viewType == VIEW_TYPE_IMAGE_SENT) {
            return new SendImageMessageViewHolder(
                    ItemContainerSendMessageImageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedImageMessageViewHolder(
                    ItemContainerReceivedMessageImageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = chatMessages.get(position);
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SendMessageViewHolder) holder).setDate(message);
        } else if (getItemViewType(position) == VIEW_TYPE_IMAGE_SENT) {
            ((SendImageMessageViewHolder) holder).setImage(message);
        } else if (getItemViewType(position) == VIEW_TYPE_RECEIVED) {
            ((ReceivedMessageViewHolder) holder).setData(message, receiverProfileImage);
        } else {
            ((ReceivedImageMessageViewHolder) holder).setImage(message, receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        boolean isImage = message.isImage;

        int viewType;
        if (message.senderId.equals(senderId)) {
            viewType = isImage ? VIEW_TYPE_IMAGE_SENT : VIEW_TYPE_SENT;
        } else {
            viewType = isImage ? VIEW_TYPE_IMAGE_RECEIVED : VIEW_TYPE_RECEIVED;
        }

        return viewType;
    }

    static class SendMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerSendMessageBinding binding;

        SendMessageViewHolder(ItemContainerSendMessageBinding itemContainerSendMessageBinding) {
            super(itemContainerSendMessageBinding.getRoot());
            binding = itemContainerSendMessageBinding;
        }

        void setDate(ChatMessage chatMessage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerReceivedMessageBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
            binding.imageProfile.setImageBitmap(receiverProfileImage);
        }
    }

    static class SendImageMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerSendMessageImageBinding binding;

        SendImageMessageViewHolder(ItemContainerSendMessageImageBinding itemContainerSendMessageImageBinding) {
            super(itemContainerSendMessageImageBinding.getRoot());
            binding = itemContainerSendMessageImageBinding;
        }

        void setImage(ChatMessage chatMessage) {
            byte[] decodedString = Base64.decode(chatMessage.message, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            binding.imageMessage.setImageBitmap(decodedByte);
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }

    static class ReceivedImageMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerReceivedMessageImageBinding binding;

        ReceivedImageMessageViewHolder(ItemContainerReceivedMessageImageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setImage(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            byte[] decodedString = Base64.decode(chatMessage.message, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            binding.imageMessage.setImageBitmap(decodedByte);
            binding.textDateTime.setText(chatMessage.dateTime);
            binding.imageProfile.setImageBitmap(receiverProfileImage);
        }
    }
}
