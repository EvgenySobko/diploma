package com.evgenysobko.diploma.services

import com.intellij.openapi.project.Project
import com.evgenysobko.diploma.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
