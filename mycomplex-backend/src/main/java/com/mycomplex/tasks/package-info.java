@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {
                "users::api", "notifications::api", "observability::api"
        }
)
package com.mycomplex.tasks;
