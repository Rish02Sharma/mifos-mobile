package org.mifos.mobilebanking.ui.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mifos.mobilebanking.R;
import org.mifos.mobilebanking.api.local.PreferencesHelper;
import org.mifos.mobilebanking.models.client.Client;
import org.mifos.mobilebanking.presenters.HomeOldPresenter;
import org.mifos.mobilebanking.ui.activities.HomeActivity;
import org.mifos.mobilebanking.ui.activities.LoanApplicationActivity;
import org.mifos.mobilebanking.ui.activities.UserProfileActivity;
import org.mifos.mobilebanking.ui.activities.base.BaseActivity;
import org.mifos.mobilebanking.ui.enums.AccountType;
import org.mifos.mobilebanking.ui.enums.ChargeType;
import org.mifos.mobilebanking.ui.fragments.base.BaseFragment;
import org.mifos.mobilebanking.ui.views.HomeOldView;
import org.mifos.mobilebanking.utils.Constants;
import org.mifos.mobilebanking.utils.CurrencyUtil;
import org.mifos.mobilebanking.utils.MaterialDialog;
import org.mifos.mobilebanking.utils.Toaster;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by michaelsosnick on 1/1/17.
 */

public class HomeOldFragment extends BaseFragment implements HomeOldView,
        SwipeRefreshLayout.OnRefreshListener {

    public static final String LOG_TAG = HomeFragment.class.getSimpleName();

    @BindView(R.id.tv_saving_total_amount)
    TextView tvSavingTotalAmount;

    @BindView(R.id.tv_loan_total_amount)
    TextView tvLoanTotalAmount;

    @BindView(R.id.ll_account_detail)
    LinearLayout llAccountDetail;

    @BindView(R.id.iv_visibility)
    ImageView ivVisibility;

    @BindView(R.id.iv_user_image)
    ImageView ivUserImage;

    @BindView(R.id.tv_user_name)
    TextView tvUserName;

    @BindView(R.id.swipe_home_container)
    SwipeRefreshLayout slHomeContainer;

    @Inject
    HomeOldPresenter presenter;

    @Inject
    PreferencesHelper preferencesHelper;

    View rootView;
    private double totalLoanAmount, totalSavingAmount;
    private Bitmap userProfileBitmap;
    private Client client;
    private long clientId;
    private View toolbarView;
    private boolean isDetailVisible;

    public static HomeOldFragment newInstance() {
        HomeOldFragment fragment = new HomeOldFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home_old, container, false);
        ((HomeActivity) getActivity()).getActivityComponent().inject(this);
        ButterKnife.bind(this, rootView);
        clientId = preferencesHelper.getClientId();

        presenter.attachView(this);
        slHomeContainer.setColorSchemeResources(R.color.blue_light, R.color.green_light, R
                .color.orange_light, R.color.red_light);
        slHomeContainer.setOnRefreshListener(this);
        if (savedInstanceState == null) {
            loadClientData();
        }

        setToolbarTitle(getString(R.string.home));
        showUserInterface();
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(Constants.TOTAL_LOAN, totalLoanAmount);
        outState.putDouble(Constants.TOTAL_SAVINGS, totalSavingAmount);
        outState.putParcelable(Constants.USER_PROFILE, userProfileBitmap);
        outState.putParcelable(Constants.USER_DETAILS, client);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            showUserDetails((Client) savedInstanceState.getParcelable(Constants.USER_DETAILS));
            showUserImage((Bitmap) savedInstanceState.getParcelable(Constants.USER_PROFILE));
            showLoanAccountDetails(savedInstanceState.getDouble(Constants.TOTAL_LOAN));
            showSavingAccountDetails(savedInstanceState.getDouble(Constants.TOTAL_SAVINGS));
        }
    }

    @Override
    public void onRefresh() {
        loadClientData();
    }

    private void loadClientData() {
        presenter.loadClientAccountDetails();
        presenter.getUserDetails();
        presenter.getUserImage();
    }

    @Override
    public void showUserInterface() {
        toolbarView = ((HomeActivity) getActivity()).getToolbar().getRootView();
        isDetailVisible = preferencesHelper.overviewState();
        if (isDetailVisible) {
            showOverviewState();
        } else {
            hideOverviewState();
        }
    }

    /**
     * Opens {@link ClientAccountsFragment} according to the {@code accountType} provided
     * @param accountType Enum of {@link AccountType}
     */
    public void openAccount(AccountType accountType) {
        ((BaseActivity) getActivity()).replaceFragment(
                ClientAccountsFragment.newInstance(accountType), true, R.id.container);
    }

    /**
     * Provides {@code totalLoanAmount} fetched from server
     * @param totalLoanAmount Total Loan amount
     */
    @Override
    public void showLoanAccountDetails(double totalLoanAmount) {
        this.totalLoanAmount = totalLoanAmount;
        tvLoanTotalAmount.setText(CurrencyUtil.formatCurrency(getContext(), totalLoanAmount));
    }

    /**
     * Open LOAN tab under ClientAccountsFragment
     */
    @OnClick(R.id.ll_total_loan)
    public void onClickLoan() {
        openAccount(AccountType.LOAN);
        ((HomeActivity) getActivity()).setNavigationViewSelectedItem(R.id.item_accounts);
    }
    /**
     * Provides {@code totalSavingAmount} fetched from server
     * @param totalSavingAmount Total Saving amount
     */
    @Override
    public void showSavingAccountDetails(double totalSavingAmount) {
        this.totalSavingAmount = totalSavingAmount;
        tvSavingTotalAmount.setText(CurrencyUtil.formatCurrency(getContext(), totalSavingAmount));
    }

    /**
     * Open SAVINGS tab under ClientAccountsFragment
     */
    @OnClick(R.id.ll_total_savings)
    public void onClickSavings() {
        openAccount(AccountType.SAVINGS);
        ((HomeActivity) getActivity()).setNavigationViewSelectedItem(R.id.item_accounts);
    }

    /**
     * Fetches Client details and display clientName
     * @param client Details about client
     */
    @Override
    public void showUserDetails(Client client) {
        this.client = client;
        tvUserName.setText(getString(R.string.hello_client, client.getDisplayName()));
    }

    /**
     * Provides with Client image fetched from server
     * @param bitmap Client Image
     */
    @Override
    public void showUserImage(final Bitmap bitmap) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                userProfileBitmap = bitmap;
                ivUserImage.setImageBitmap(bitmap);
            }
        });
    }

    @OnClick(R.id.iv_user_image)
    public void userImageClicked() {
        startActivity(new Intent(getActivity(), UserProfileActivity.class));
    }
    /**
     * Reverses the state of Account Overview section i.e. visible to hidden or vice a versa
     */
    @OnClick(R.id.iv_visibility)
    public void reverseDetailState() {
        if (isDetailVisible) {
            isDetailVisible = false;
            preferencesHelper.setOverviewState(false);
            hideOverviewState();
        } else {
            isDetailVisible = true;
            preferencesHelper.setOverviewState(true);
            showOverviewState();
        }
    }

    /**
     * Makes Overview state visible
     */
    private void showOverviewState() {
        ivVisibility.setColorFilter(ContextCompat.getColor(getActivity(), R.color.gray_dark));
        llAccountDetail.setVisibility(View.VISIBLE);
    }

    /**
     * Hides Overview state
     */
    private void hideOverviewState() {
        ivVisibility.setColorFilter(ContextCompat.getColor(getActivity(), R.color.light_grey));
        llAccountDetail.setVisibility(View.GONE);
    }


    /**
     * Calls {@code openAccount()} for opening {@link ClientAccountsFragment}
     */
    @OnClick(R.id.ll_accounts)
    public void accountsClicked() {
        openAccount(AccountType.SAVINGS);
        ((HomeActivity) getActivity()).setNavigationViewSelectedItem(R.id.item_accounts);
    }

    /**
     * Shows a dialog with options: Normal Transfer and Third Party Transfer
     */
    @OnClick(R.id.ll_transfer)
    public void transferClicked() {
        String[] transferTypes = {getString(R.string.transfer), getString(R.string.
                third_party_transfer)};
        new MaterialDialog.Builder().init(getActivity())
                .setTitle(R.string.choose_transfer_type)
                .setItems(transferTypes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            ((HomeActivity) getActivity()).replaceFragment(
                                    SavingsMakeTransferFragment.newInstance(1, ""), true,
                                    R.id.container);
                        } else {
                            ((HomeActivity) getActivity()).replaceFragment(
                                    ThirdPartyTransferFragment.newInstance(), true, R.id.container);
                        }
                    }
                })
                .createMaterialDialog()
                .show();
    }

    /**
     * Opens {@link ClientChargeFragment} to display all Charges associated with client's account
     */
    @OnClick(R.id.ll_charges)
    public void chargesClicked() {
        ((HomeActivity) getActivity()).replaceFragment(ClientChargeFragment.newInstance(clientId,
                ChargeType.CLIENT), true,  R.id.container);
    }

    /**
     * Opens up Phone Dialer
     */
    @OnClick(R.id.tv_help_contact)
    void dialHelpLineNumber() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + getString(R.string.help_line_number)));
        startActivity(intent);
    }

    /**
     * Opens {@link LoanApplicationFragment} to apply for a loan
     */
    @OnClick(R.id.ll_apply_for_loan)
    public void applyForLoan() {
        startActivity(new Intent(getActivity(), LoanApplicationActivity.class));
    }

    /**
     * Opens {@link BeneficiaryListFragment} which contains list of Beneficiaries associated with
     * Client's account
     */
    @OnClick(R.id.ll_beneficiaries)
    public void beneficiaries() {
        ((HomeActivity) getActivity()).replaceFragment(BeneficiaryListFragment.
                newInstance(), true,  R.id.container);
    }

    @OnClick(R.id.ll_surveys)
    public void surveys() {

    }

    /**
     * It is called whenever any error occurs while executing a request
     * @param errorMessage Error message that tells the user about the problem.
     */
    @Override
    public void showError(String errorMessage) {
        Toaster.show(rootView, errorMessage);
    }

    /**
     * Shows {@link SwipeRefreshLayout}
     */
    @Override
    public void showProgress() {
        slHomeContainer.setRefreshing(true);
    }

    /**
     * Hides {@link SwipeRefreshLayout}
     */
    @Override
    public void hideProgress() {
        slHomeContainer.setRefreshing(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (slHomeContainer.isRefreshing()) {
            slHomeContainer.setRefreshing(false);
            slHomeContainer.removeAllViews();
        }
        presenter.detachView();
    }

}

