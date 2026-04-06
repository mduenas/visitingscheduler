package com.markduenas.visischeduler.platform

import dev.gitlive.firebase.storage.Data

/** Convert a [ByteArray] to the platform storage [Data] type. */
expect fun ByteArray.toStorageData(): Data
