# TODO List for Enhancing Java Chat Application

## New Files to Create
- [ ] Create Message.java: Define a Message class with fields for content, timestamp, sender, type (public/private), and encryption support.
- [ ] Create EncryptionUtil.java: Implement simple XOR encryption/decryption for messages to add uniqueness.

## Edit ChatServer.java
- [ ] Add private messaging support: Handle /pm commands to send direct messages to specific users.
- [ ] Send online user list: Broadcast user list updates on join/leave.
- [ ] Add timestamps to messages: Include timestamp in broadcasted messages.
- [ ] Implement message encryption: Encrypt messages before broadcasting.
- [ ] Handle file sharing: Support sending/receiving files between clients.

## Edit ChatClientFX.java
- [ ] Add UI elements: User list pane, emoji picker, file send button.
- [ ] Support private message tabs/windows: Allow opening private chats.
- [ ] Display timestamps: Show timestamps in message area.
- [ ] Add typing indicators: Notify when a user is typing.
- [ ] Add theme toggle: Light/dark mode switch.
- [ ] Handle file receiving and saving: Allow downloading received files.

## Followup Steps
- [ ] Test compilation and running of server and client.
- [ ] Verify private messaging functionality.
- [ ] Verify file sharing functionality.
- [ ] Verify encryption and other features.
