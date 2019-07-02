/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.pushers

import arrow.core.Try
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.pushers.Pusher
import im.vector.matrix.android.api.session.pushers.PusherState
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.PusherEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.task.Task
import im.vector.matrix.android.internal.util.tryTransactionSync
import javax.inject.Inject

internal interface RemovePusherTask : Task<RemovePusherTask.Params, Unit> {
    data class Params(val userId: String,
                      val pushKey: String,
                      val pushAppId: String)
}

internal class DefaultRemovePusherTask @Inject constructor(
        private val pushersAPI: PushersAPI,
        private val monarchy: Monarchy
) : RemovePusherTask {

    override suspend fun execute(params: RemovePusherTask.Params): Try<Unit> {
        return Try {
            var existing: Pusher? = null
            monarchy.runTransactionSync {
                val existingEntity = PusherEntity.where(it, params.userId, params.pushKey).findFirst()
                existingEntity?.state == PusherState.UNREGISTERING
                existing = existingEntity?.asDomain()
            }
            if (existing == null) {
                throw Exception("No existing pusher")
            } else {
                existing!!
            }
        }.flatMap {
            executeRequest<Unit> {
                val deleteBody = JsonPusher(
                        pushKey = params.pushKey,
                        appId = params.pushAppId,
                        // kind null deletes the pusher
                        kind = null,
                        appDisplayName = it.appDisplayName ?: "",
                        deviceDisplayName = it.deviceDisplayName ?: "",
                        profileTag = it.profileTag ?: "",
                        lang = it.lang,
                        data = JsonPusherData(it.data.url, it.data.format),
                        append = false
                )
                apiCall = pushersAPI.setPusher(deleteBody)
            }
        }.flatMap {
            monarchy.tryTransactionSync {
                val existing = PusherEntity.where(it, params.userId, params.pushKey).findFirst()
                existing?.deleteFromRealm()
            }
        }
    }


}