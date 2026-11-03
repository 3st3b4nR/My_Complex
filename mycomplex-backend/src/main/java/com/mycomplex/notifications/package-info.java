@org.springframework.modulith.ApplicationModule(
        displayName = "Notifications",
        allowedDependencies = {
                "users::api",
                "observability::api",
                "news::events",
                "meetings::events"
        }
)
package com.mycomplex.notifications;
