package com.aliucord.plugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.models.message.Message
import com.discord.stores.StoreUserRelationships

@AliucordPlugin
class ShowBlockedMessages : Plugin() {
    
    override fun start(context: Context) {
        // Patch 1: Make Message.isBlocked() always return false
        patchMessageBlocked()
        
        // Patch 2: Prevent relationship store from marking users as blocked in message context
        patchRelationshipStore()
        
        // Patch 3: Patch the message rendering to skip blocked check
        patchMessageRenderer()
    }

    private fun patchMessageBlocked() {
        try {
            val messageClass = Message::class.java
            
            // Find and patch the isBlocked method
            patcher.patch(
                messageClass.getDeclaredMethod("isBlocked"),
                Hook { param ->
                    param.result = false
                }
            )
            
            logger.info("Successfully patched Message.isBlocked()")
        } catch (e: Throwable) {
            logger.error("Failed to patch Message.isBlocked()", e)
        }
    }

    private fun patchRelationshipStore() {
        try {
            // Patch the StoreUserRelationships to not block messages
            val storeClass = StoreUserRelationships::class.java
            
            // Patch isBlocked method
            patcher.patch(
                storeClass.getDeclaredMethod("isBlocked", Long::class.javaPrimitiveType),
                Hook { param ->
                    // Return false to indicate user is not blocked
                    param.result = false
                }
            )
            
            logger.info("Successfully patched StoreUserRelationships.isBlocked()")
        } catch (e: Throwable) {
            logger.error("Failed to patch StoreUserRelationships", e)
        }
    }

    private fun patchMessageRenderer() {
        try {
            // Patch the widget that renders chat messages
            val widgetChatListClass = Class.forName("com.discord.widgets.chat.list.WidgetChatList")
            
            // Find methods that check if message is blocked
            widgetChatListClass.declaredMethods.forEach { method ->
                if (method.name.contains("blocked", ignoreCase = true) && 
                    method.returnType == Boolean::class.javaPrimitiveType) {
                    
                    patcher.patch(method, Hook { param ->
                        param.result = false
                    })
                    
                    logger.info("Patched method: ${method.name}")
                }
            }
            
        } catch (e: Throwable) {
            logger.error("Failed to patch message renderer", e)
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}