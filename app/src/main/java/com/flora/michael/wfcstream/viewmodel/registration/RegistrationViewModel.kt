package com.flora.michael.wfcstream.viewmodel.registration

import android.app.Application
import androidx.lifecycle.*
import com.flora.michael.wfcstream.model.resultCode.authorization.RegisterResultCode
import com.flora.michael.wfcstream.repository.AuthorizationRepository
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class RegistrationViewModel(application: Application): AndroidViewModel(application), KodeinAware{
    override val kodein: Kodein by closestKodein()
    private val authorizationRepository: AuthorizationRepository by instance()

    private val loginMutable = MutableLiveData<String>()
    private val userNameMutable = MutableLiveData<String>()
    private val passwordMutable = MutableLiveData<String>()
    private val confirmPasswordMutable = MutableLiveData<String>()
    private val registerResultMutable = MutableLiveData<RegisterResultCode>()

    private val loginErrorMutable = MutableLiveData<String>()
    private val userNameErrorMutable = MutableLiveData<String>()
    private val passwordErrorMutable = MutableLiveData<String>()
    private val confirmPasswordErrorMutable = MutableLiveData<String>()

    val login: LiveData<String> = loginMutable
    val userName: LiveData<String> = userNameMutable
    val password: LiveData<String> = passwordMutable
    val confirmPassword: LiveData<String> = confirmPasswordMutable
    val registrationResult: LiveData<RegisterResultCode> = registerResultMutable

    val loginError: LiveData<String> = loginErrorMutable
    val userNameError: LiveData<String> = userNameErrorMutable
    val passwordError: LiveData<String> = passwordErrorMutable
    val confirmPasswordError: LiveData<String> = confirmPasswordErrorMutable
    val isErrorsExist: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {

        fun onLiveDataValueChanged(changedLiveData: LiveData<*>){
            value = !(checkIsLoginCorrect(changedLiveData == login) and
                    checkIsUserNameCorrect(changedLiveData == userName) and
                    checkIsPasswordCorrect(changedLiveData == password || changedLiveData == confirmPassword) and
                    checkIsConfirmPasswordCorrect(changedLiveData == confirmPassword || changedLiveData == password))
        }

        addSource(login){
            onLiveDataValueChanged(login)
        }
        addSource(userName){
            onLiveDataValueChanged(userName)
        }
        addSource(password){
            onLiveDataValueChanged(password)
        }
        addSource(confirmPassword){
            onLiveDataValueChanged(confirmPassword)
        }
    }

    fun updateLogin(enteredLogin: String){
        loginMutable.value = enteredLogin
        //checkIsLoginCorrect(isWithErrorMessage = true)
    }

    fun updateUserName(enteredUserName: String){
        userNameMutable.value = enteredUserName
        //checkIsUserNameCorrect(isWithErrorMessage = true)
    }

    fun updatePassword(enteredPassword: String){
        passwordMutable.value = enteredPassword
        //checkIsPasswordCorrect(isWithErrorMessage = true)
    }

    fun updateConfirmPassword(enteredPassword: String){
        confirmPasswordMutable.value = enteredPassword
        //checkIsConfirmPasswordCorrect(isWithErrorMessage = true)
    }

    fun register(){
        if(!checkAllDataCorrect(withErrorMessage = true))
            return

        login.value?.let{ login ->
            userName.value?.let { userName ->
                password.value?.let { password ->
                    viewModelScope.launch {
                        registerResultMutable.value = authorizationRepository.register(login, password, userName)
                    }
                }
            }
        }
    }

    private fun checkIsLoginCorrect(isWithErrorMessage: Boolean = false): Boolean{
        val login = this.login.value

        if(login == null || login.isEmpty()){
            if(isWithErrorMessage){
                loginErrorMutable.value = "Поле является обязательным."
            }

            return false
        }

        if(!login.matches(LOGIN_REGULAR_EXPRESSION)){
            if(isWithErrorMessage) {
                loginErrorMutable.value = "Недопустимые символы."
            }
            return false
        }

        if(login.length < MIN_LOGIN_LENGTH){
            if(isWithErrorMessage){
                loginErrorMutable.value = "Минимум $MIN_LOGIN_LENGTH символов."
            }

            return false
        }

        if(login.length > MAX_LOGIN_LENGTH){
            if(isWithErrorMessage) {
                loginErrorMutable.value = "Максимум $MAX_LOGIN_LENGTH символов."
            }

            return false
        }

        loginErrorMutable.value = null
        return true
    }

    private fun checkIsUserNameCorrect(isWithErrorMessage: Boolean = false): Boolean{
        val userName = this.userName.value

        if(userName == null || userName.isEmpty()){
            if(isWithErrorMessage) {
                userNameErrorMutable.value = "Поле является обязательным."
            }

            return false
        }

        if(!userName.matches(USERNAME_REGULAR_EXPRESSION)){
            if(isWithErrorMessage) {
                userNameErrorMutable.value = "Недопустимые символы."
            }

            return false
        }

        if(userName.length < MIN_USER_NAME_LENGTH){
            if(isWithErrorMessage) {
                userNameErrorMutable.value = "Минимум $MIN_LOGIN_LENGTH символов."
            }

            return false
        }

        if(userName.length > MAX_USER_NAME_LENGTH){
            if(isWithErrorMessage) {
                userNameErrorMutable.value = "Максимум $MAX_LOGIN_LENGTH символов."
            }

            return false
        }

        userNameErrorMutable.value = null
        return true
    }

    private fun checkIsPasswordCorrect(isWithErrorMessage: Boolean = false): Boolean{
        val password = this.password.value

        if(password == null || password.isEmpty()){
            if(isWithErrorMessage) {
                passwordErrorMutable.value = "Поле является обязательным."
            }

            return false
        }

        if(password.length > MAX_PASSWORD_LENGTH){
            if(isWithErrorMessage) {
                passwordErrorMutable.value = "Максимум $MAX_PASSWORD_LENGTH символов."
            }

            return false
        }

        passwordErrorMutable.value = null
        return true
    }

    private fun checkIsConfirmPasswordCorrect(isWithErrorMessage: Boolean = false): Boolean{
        val password = this.password.value
        val confirmPassword = this.confirmPassword.value

        if(password != confirmPassword){
            if(isWithErrorMessage) {
                confirmPasswordErrorMutable.value = "Пароли не совпадают."
            }

            return false
        }

        if(confirmPassword == null || confirmPassword.isEmpty()){
            if(isWithErrorMessage) {
                confirmPasswordErrorMutable.value = "Поле является обязательным."
            }

            return false
        }

        confirmPasswordErrorMutable.value = null
        return true
    }

    private fun checkAllDataCorrect(withErrorMessage: Boolean = false): Boolean{
        return checkIsLoginCorrect(withErrorMessage) and
                checkIsUserNameCorrect(withErrorMessage) and
                checkIsPasswordCorrect(withErrorMessage) and
                checkIsConfirmPasswordCorrect(withErrorMessage)
    }

    companion object{
        const val MIN_LOGIN_LENGTH = 5
        const val MAX_LOGIN_LENGTH = 15

        const val MIN_USER_NAME_LENGTH = 5
        const val MAX_USER_NAME_LENGTH = 15

        const val MAX_PASSWORD_LENGTH = 100

        private val LOGIN_REGULAR_EXPRESSION = Regex("[A-Z0-9_]+", RegexOption.IGNORE_CASE)
        private val USERNAME_REGULAR_EXPRESSION = Regex("[A-Z0-9_]+", RegexOption.IGNORE_CASE)
    }

}