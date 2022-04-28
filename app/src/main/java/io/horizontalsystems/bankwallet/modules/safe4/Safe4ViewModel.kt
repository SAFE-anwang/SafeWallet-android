package io.horizontalsystems.bankwallet.modules.safe4

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.anwang.safewallet.safekit.model.SafeChain
import com.anwang.safewallet.safekit.model.SafeNet
import io.horizontalsystems.bankwallet.core.App
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class Safe4ViewModel() : ViewModel() {

    private val disposables = CompositeDisposable()

    fun getSafeNet(safe2eth: Boolean, navController: NavController) {
        App.safeProvider.getSafeNet()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.i("safe4", "getSafeNet data: $it")
                if(safe2eth){
                    Safe4Module.handlerSafe2eth(it)
                } else {
                    Safe4Module.handlerEth2safe(it, navController)
                }
            }, {
                Log.e("safe4", "getSafeNet error", it)
            })
            .let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        disposables.clear()
    }

}
