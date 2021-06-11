package com.evgenysobko.diploma.tracer

import com.evgenysobko.diploma.agent.TracerTrampoline
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.coverage.org.objectweb.asm.Opcodes.ASM8
import org.objectweb.asm.*
import org.objectweb.asm.ClassReader.EXPAND_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import kotlin.reflect.jvm.javaMethod

private const val ASM_API = ASM8

class TracerClassFileTransformer : ClassFileTransformer {

    override fun transform(
        loader: ClassLoader?,
        classJvmName: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray,
    ): ByteArray? {
        return try {
            val className = classJvmName.replace('/', '.')
            if (TracerConfig.shouldInstrumentClass(className)) {
                tryTransform(className, classfileBuffer)
            } else {
                null
            }
        } catch (e: Throwable) {
            //Logger.getInstance(javaClass).error("Failed to instrument class $classJvmName", e)
            null
        }
    }

    private fun tryTransform(
        clazz: String,
        classBytes: ByteArray
    ): ByteArray? {
        val reader = ClassReader(classBytes)
        val writer = ClassWriter(reader, COMPUTE_MAXS)
        val classVisitor = TracerClassVisitor(clazz, writer)
        reader.accept(classVisitor, EXPAND_FRAMES)
        return when {
            classVisitor.transformedSomeMethods -> writer.toByteArray()
            else -> null
        }
    }
}

class TracerClassVisitor(
    private val clazz: String,
    writer: ClassVisitor,
) : ClassVisitor(ASM_API, writer) {
    var transformedSomeMethods = false

    override fun visitMethod(
        access: Int, method: String, desc: String, signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor? {
        val methodWriter = super.visitMethod(access, method, desc, signature, exceptions)
        val methodFqName = MethodFqName(clazz, method, desc)
        val traceData = TracerConfig.getMethodTraceData(methodFqName)
        if (traceData != null && traceData.config.enabled) {
            transformedSomeMethods = true
            return TracerMethodVisitor(methodWriter, traceData, clazz, method, desc, access)
        } else {
            return methodWriter
        }
    }
}

class TracerMethodVisitor(
    methodWriter: MethodVisitor,
    private val traceData: MethodTraceData,
    private val clazz: String,
    private val method: String,
    private val desc: String,
    access: Int,
) : AdviceAdapter(ASM_API, methodWriter, access, method, desc) {

    companion object {
        private val LOG = Logger.getInstance(TracerMethodVisitor::class.java)
        private val OBJECT_TYPE = Type.getType(Any::class.java)
        private val THROWABLE_TYPE = Type.getType(Throwable::class.java)
        private val TRAMPOLINE_TYPE = Type.getType(TracerTrampoline::class.java)
        private val TRAMPOLINE_ENTER_METHOD = Method.getMethod(TracerTrampoline::enter.javaMethod)
        private val TRAMPOLINE_LEAVE_METHOD = Method.getMethod(TracerTrampoline::leave.javaMethod)
    }

    private val methodStart = newLabel()

    private var methodEntered = false

    override fun onMethodEnter() {
        push(traceData.methodId)
        loadTracedArgs()
        invokeStatic(TRAMPOLINE_TYPE, TRAMPOLINE_ENTER_METHOD)
        methodEntered = true
        mark(methodStart)
    }

    override fun onMethodExit(opcode: Int) {
        if (methodEntered && opcode != ATHROW) {
            invokeStatic(TRAMPOLINE_TYPE, TRAMPOLINE_LEAVE_METHOD)
        }
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        if (methodEntered) {
            buildCatchBlock()
        } else {
            val fqName = "$clazz.$method$desc"
            LOG.warn("Unable to instrument $fqName because ASM failed to call onMethodEnter")
        }
        super.visitMaxs(maxStack, maxLocals)
    }

    private fun buildCatchBlock() {
        catchException(methodStart, mark(), THROWABLE_TYPE)
        visitFrame(F_NEW, 0, emptyArray(), 1, arrayOf(THROWABLE_TYPE.internalName))
        invokeStatic(TRAMPOLINE_TYPE, TRAMPOLINE_LEAVE_METHOD)
        throwException() // Rethrow.
    }

    private fun loadTracedArgs() {
        val rawTracedParams = traceData.config.tracedParams
        val tracedParams = rawTracedParams.filter(argumentTypes.indices::contains)
        if (tracedParams.size < rawTracedParams.size) {
            val fqName = "$clazz.$method$desc"
            LOG.warn("Some arg indices are out of bounds for method $fqName: $rawTracedParams")
        }

        if (tracedParams.isEmpty()) {
            visitInsn(ACONST_NULL)
            return
        }

        push(tracedParams.size)
        newArray(OBJECT_TYPE)
        for ((storeIndex, paramIndex) in tracedParams.withIndex()) {
            dup()
            push(storeIndex)
            loadArg(paramIndex)
            box(argumentTypes[paramIndex])
            arrayStore(OBJECT_TYPE)
        }
    }
}
