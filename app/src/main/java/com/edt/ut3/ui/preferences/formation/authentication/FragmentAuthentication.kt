package com.edt.ut3.ui.preferences.formation.authentication

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.edt.ut3.R
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.misc.extensions.discard
import com.edt.ut3.misc.extensions.hideKeyboard
import com.edt.ut3.misc.extensions.isTrue
import com.edt.ut3.misc.extensions.updateIfNecessary
import com.edt.ut3.ui.custom_views.TextInputEditText2
import com.edt.ut3.ui.preferences.formation.FormationSelectionViewModel
import com.edt.ut3.ui.preferences.formation.state_fragment.StateFragment
import kotlinx.android.synthetic.main.fragment_authentication.*
import kotlinx.android.synthetic.main.state_fragment.*

class FragmentAuthentication: Fragment() {

    val viewModel: FormationSelectionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_authentication, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners(view.context)
    }

    /**
     * Setup the fragment listeners.
     *
     * @param context A valid [Context]
     */
    private fun setupListeners(context: Context) {
        viewModel.run {
            getCredentials(context).observe(viewLifecycleOwner, ::handleCredentialsUpdate)
            authenticationState.observe(viewLifecycleOwner, ::handleStateChange)
            authenticationFailure.observe(viewLifecycleOwner, ::handleFailure)

        }

        setupField(username)
        setupField(password)
    }

    /**
     * Setup a credential field in order to
     * handle the changes from it and update
     * the UI depending on them.
     *
     * @param field The text field
     */
    private fun setupField(field: TextInputEditText2?) = field?.apply {
        doOnTextChanged { _, _, _, _ -> updateViewModelCredentials() }

        setOnKeyListener { v, keyCode, event ->
            event.takeIf { it.action == KeyEvent.ACTION_UP }?.let {
                showKeyboardOnEvent(v, keyCode)
            }

            false
        }

        setOnFocusChangeListener { _, _ ->
            showActionButtonsDependingOnFocus()
        }

        onPreImeListener = { code: Int, event: KeyEvent? ->
            val shouldHandle = (code == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP)
            if (shouldHandle) {
                hideKeyboard()
                clearFocus()
                showActionButtonsIfSubFragment()
            }

            shouldHandle
        }
    }

    /**
     * Shows the parent's action buttons
     * depending on the current event.
     *
     * @param v The view
     * @param keyCode The event key code
     * @return Always return false
     */
    private fun showKeyboardOnEvent(v: View? = null, keyCode: Int): Boolean {
        val shouldBeHandled = (keyCode == KeyEvent.KEYCODE_ENTER && v == password)

        if (shouldBeHandled) {
            showActionButtonsIfSubFragment()
        }

        return false
    }

    /**
     * Show the actions button depending
     * on the credentials fields focuses.
     */
    private fun showActionButtonsDependingOnFocus() {
        when {
            username?.hasFocus().isTrue() -> hideActionButtonsIfSubFragment()
            password?.hasFocus().isTrue() -> hideActionButtonsIfSubFragment()
            else -> {}
        }
    }

    /**
     * Hides the action buttons if the
     * current fragment is a child of
     * a StateFragment.
     */
    private fun hideActionButtonsIfSubFragment() {
        val parent = parentFragment
        if (parent is StateFragment) {
            parent.action_buttons?.visibility = GONE
        }
    }

    /**
     * Show the action buttons if the
     * current fragment is a child of
     * a StateFragment.
     */
    private fun showActionButtonsIfSubFragment() {
        val parent = parentFragment
        if (parent is StateFragment) {
            parent.action_buttons?.visibility = VISIBLE
        }
    }

    /**
     * Sets the credentials into the [viewModel].
     * If one of the field is null or blank,
     * we just pass null to the [viewModel].
     */
    private fun updateViewModelCredentials() {
        val username = username?.text.takeIf { !it.isNullOrBlank() }
        val password = password?.text.takeIf { !it.isNullOrBlank() }

        viewModel.updateCredentials(
            Authenticator.Credentials.from(
                username?.toString(),
                password?.toString()
            )
        )
    }

    /**
     * Update the view depending on the
     * incoming [credentials].
     *
     * If they are the same as the view's ones,
     * they are not updated to avoid an infinite
     * loop.
     *
     * @param credentials The incoming credentials
     */
    private fun handleCredentialsUpdate(credentials: Authenticator.Credentials?) {
        val newUsername = credentials?.username ?: return
        val newPassword = credentials.password

        username?.updateIfNecessary(newUsername)
        password?.updateIfNecessary(newPassword)
    }

    /**
     * Handles an [AuthenticationFailure].
     * Actually it only display a [Toast].
     *
     * @param failure The incoming failure.
     */
    private fun handleFailure(failure: AuthenticationFailure?) = failure?.let {
        context?.let {
            Toast.makeText(it, failure.reason(it), Toast.LENGTH_SHORT).show()
        }

        viewModel.clearFailure(failure)
    }

    /**
     * Update the view depending on
     * incoming [state].
     *
     * @param state The new state.
     */
    private fun handleStateChange(state: AuthenticationState?): Unit = when (state) {
        AuthenticationState.Unauthenticated -> {
            username?.isEnabled = true
            password?.isEnabled = true

            context?.let {
                val parent = parentFragment
                val credentials = viewModel.getCredentials(it).value
                if (parent is StateFragment) {
                    when (credentials) {
                        null -> parent.setNextText(R.string.step_skip)
                        else -> parent.setNextText(R.string.step_check_credentials)
                    }
                }
            }
        }

        AuthenticationState.Authenticating -> {
            val parent = parentFragment
            if (parent is StateFragment) {
                parent.setNextText(R.string.step_checking_credentials)
            }

            username?.isEnabled = false
            password?.isEnabled = false
        }

        AuthenticationState.Authenticated -> {
            val parent = parentFragment
            if (parent is StateFragment) {
                parent.resetNextText()
            }

            username?.isEnabled = true
            password?.isEnabled = true
        }

        else -> {}
    }.discard()
}
