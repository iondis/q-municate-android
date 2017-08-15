package com.quickblox.q_municate.ui.activities.invitefriends;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.quickblox.q_municate.R;
import com.quickblox.q_municate.ui.adapters.invitefriends.ImportFriendAdapter;
import com.quickblox.q_municate.utils.DialogsUtils;
import com.quickblox.q_municate.utils.helpers.ImportContactsHelper;
import com.quickblox.q_municate.utils.listeners.CounterChangedListener;
import com.quickblox.q_municate.ui.activities.base.BaseLoggableActivity;
import com.quickblox.q_municate.ui.adapters.invitefriends.InviteFriendsAdapter;
import com.quickblox.q_municate.utils.ToastUtils;
import com.quickblox.q_municate.utils.helpers.EmailHelper;
import com.quickblox.q_municate.utils.listeners.simple.SimpleActionModeCallback;
import com.quickblox.q_municate_core.core.command.Command;
import com.quickblox.q_municate_core.models.InviteFriend;
import com.quickblox.q_municate_core.service.QBServiceConsts;
import com.quickblox.q_municate_core.utils.ConstsCore;
import com.quickblox.q_municate.utils.helpers.SystemPermissionHelper;
import com.quickblox.q_municate_core.utils.Utils;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

public class ImportContactsActivity extends BaseLoggableActivity implements CounterChangedListener {

    @Bind(R.id.friends_listview)
    RecyclerView friendsListView;

    private List<InviteFriend> friendsContactsList;
    private ImportFriendAdapter friendsAdapter;
    private String[] selectedContactsFriendsArray;
    private ActionMode actionMode;
    private boolean allContactsChecked;
    private SystemPermissionHelper systemPermissionHelper;
    private ImportContactsHelper importContactsHelper;
    private ImportContactsSuccessAction importContactsSuccessAction;
    private ImportContactsFailAction importContactsFailAction;

    public static void start(Context context) {
        Intent intent = new Intent(context, ImportContactsActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int getContentResId() {
        return R.layout.activity_invite_friends;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFields();
        setUpActionBarWithUpButton();
        addActions();
    }

    private void initImportContactsTask() {
        showProgress();
        friendsContactsList.addAll(EmailHelper.getContactsWithPhone(this));
        friendsContactsList.addAll(EmailHelper.getContactsWithEmail(this));
        importContactsHelper.startGetFriendsListTask(false);
    }

    private void addActions(){
        addAction(QBServiceConsts.IMPORT_FRIENDS_SUCCESS_ACTION, importContactsSuccessAction);
        addAction(QBServiceConsts.IMPORT_FRIENDS_FAIL_ACTION, importContactsFailAction);
    }

    protected void removeActions() {
        removeAction(QBServiceConsts.IMPORT_FRIENDS_SUCCESS_ACTION);
        removeAction(QBServiceConsts.IMPORT_FRIENDS_FAIL_ACTION);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPermissionsAndInitFriendsListIfPossible();
    }

    private void checkPermissionsAndInitFriendsListIfPossible() {
        if (systemPermissionHelper.isAllPermissionsGrantedForImportFriends()){
//            initFriendsList();
            initImportContactsTask();
        } else {
            systemPermissionHelper.requestPermissionsForImportFriends();
        }
    }

    @Override
    public void onCounterContactsChanged(int valueCounterContacts) {
        if (valueCounterContacts != ConstsCore.ZERO_INT_VALUE) {
            startActionMode();
        } else {
            stopActionMode();
        }
    }

    private void initFields() {
        title = getString(R.string.import_contacts_title);
        systemPermissionHelper = new SystemPermissionHelper(this);
        importContactsHelper = new ImportContactsHelper(this);
        importContactsSuccessAction = new ImportContactsSuccessAction();
        importContactsFailAction = new ImportContactsFailAction();
        friendsContactsList = new ArrayList<>();

    }

//    private void initFriendsList() {
//        friendsContactsList = EmailHelper.getContactsWithPhone(this);
//        friendsAdapter = new InviteFriendsAdapter(this, friendsContactsList);
//        friendsAdapter.setCounterChangedListener(this);
//        friendsListView.setAdapter(friendsAdapter);
//    }

    private void checkAllContacts() {
//        allContactsChecked = !allContactsChecked;
//        friendsAdapter.setCounterContacts(getCheckedFriends(friendsContactsList, allContactsChecked));
//        friendsAdapter.notifyDataSetChanged();
    }

    private int getCheckedFriends(List<InviteFriend> friends, boolean isCheck) {
        int newCounter;
        for (InviteFriend friend : friends) {
            friend.setSelected(isCheck);
        }
        newCounter = isCheck ? friends.size() : ConstsCore.ZERO_INT_VALUE;

        onCounterContactsChanged(newCounter);

        return newCounter;
    }

    private void startActionMode() {
        if (actionMode != null) {
            return;
        }
        actionMode = startSupportActionMode(new ActionModeCallback());
    }

    private void stopActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void performActionNext() {
        selectedContactsFriendsArray = getSelectedFriendsForInvite();

        if (selectedContactsFriendsArray.length > ConstsCore.ZERO_INT_VALUE) {
            sendInviteToContacts();
        } else {
            ToastUtils.longToast(R.string.dlg_no_contacts_selected);
        }

//        clearCheckedFriends();
    }

    private String[] getSelectedFriendsForInvite() {
        List<String> arrayList = new ArrayList<String>();
        for (InviteFriend friend : friendsContactsList) {
            if (friend.isSelected()) {
                arrayList.add(friend.getId());
            }
        }
        return arrayList.toArray(new String[arrayList.size()]);
    }

//    private void clearCheckedFriends() {
//        for (InviteFriend friend : friendsContactsList) {
//            friend.setSelected(false);
//        }
//        onCounterContactsChanged(ConstsCore.ZERO_INT_VALUE);
////        friendsAdapter.setCounterContacts(ConstsCore.ZERO_INT_VALUE);
//        friendsAdapter.notifyDataSetChanged();
//    }

    private void sendInviteToContacts() {
        EmailHelper.sendInviteEmail(this, selectedContactsFriendsArray);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case SystemPermissionHelper.PERMISSIONS_FOR_IMPORT_FRIENDS_REQUEST: {
                if (grantResults.length > 0) {
                    if (systemPermissionHelper.isAllPermissionsGrantedForImportFriends()){
//                        initFriendsList();
                        initImportContactsTask();
                    } else {
                        showPermissionSettingsDialog();
                    }
                }
            }
        }
    }

    private void showPermissionSettingsDialog() {
        DialogsUtils.showOpenAppSettingsDialog(
                getSupportFragmentManager(),
                getString(R.string.dlg_need_permission_read_contacts, getString(R.string.app_name)),
                new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        finish();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        SystemPermissionHelper.openSystemSettings(ImportContactsActivity.this);
                    }
                });
    }


    private void initFriendsList(List<QBUser> realQbFriendsList) {
        friendsAdapter = new ImportFriendAdapter(this, prepareFriendsListFromQbUsers(realQbFriendsList));
        friendsListView.setLayoutManager(new LinearLayoutManager(this));
//        friendsAdapter.setCounterChangedListener(this);
        friendsListView.setAdapter(friendsAdapter);
    }

    private List<InviteFriend> prepareFriendsListFromQbUsers(List<QBUser> realQbFriendsList) {
        List<InviteFriend> realFriendsList = new ArrayList<>(realQbFriendsList.size());

        for (QBUser qbUser : realQbFriendsList){
            InviteFriend inviteFriend = null;

            if (qbUser.getPhone() != null){
                inviteFriend = getContactById(qbUser.getPhone(), friendsContactsList);
            } else if (qbUser.getEmail() != null){
                inviteFriend = getContactById(qbUser.getEmail(), friendsContactsList);
            }

            if (inviteFriend != null) {
                inviteFriend.setQbId(qbUser.getId());
                inviteFriend.setQbName(qbUser.getFullName());
                inviteFriend.setQbAvatarUrl(Utils.customDataToObject(qbUser.getCustomData()).getAvatarUrl());
                realFriendsList.add(inviteFriend);
            }
        }

        return realFriendsList;
    }

    private InviteFriend getContactById(String inviteFriendId, List<InviteFriend> sourceList) {
        for (InviteFriend inviteFriend : sourceList){
            if (inviteFriend.getId().equals(inviteFriendId)){
                return inviteFriend;
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeActions();
    }

    private class ActionModeCallback extends SimpleActionModeCallback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.invite_friends_menu, menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_send:
                    if (checkNetworkAvailableWithError()) {
                        performActionNext();
                        actionMode.finish();
                    }
                    return true;
                case R.id.action_select_all:
//                    checkAllContacts();
                    return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
//            clearCheckedFriends();
            actionMode = null;
        }
    }


    private class ImportContactsSuccessAction implements Command{

        @Override
        public void execute(Bundle bundle) throws Exception {
            hideProgress();
            List<QBUser> realQbFriendsList = (List<QBUser>) bundle.getSerializable(QBServiceConsts.EXTRA_FRIENDS);
            initFriendsList(realQbFriendsList);
        }
    }


    private class ImportContactsFailAction implements Command{

        @Override
        public void execute(Bundle bundle) throws Exception {
            hideProgress();
        }
    }
}