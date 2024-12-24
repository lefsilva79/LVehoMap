package com.example.lvehomap

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class VehoAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VehoAccessibilityService"
        private const val VEHO_PACKAGE = "com.veho.app"
        private const val MARKER_ID_PREFIX = "route-marker-"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Serviço de acessibilidade conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName?.toString() == VEHO_PACKAGE) {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    captureDeliveryPoints(event)
                }
            }
        }
    }

    private fun captureDeliveryPoints(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        val deliveryPoints = mutableListOf<DeliveryPoint>()

        findMarkerNodes(rootNode).forEach { node ->
            val marker = extractMarkerInfo(node)
            marker?.let { deliveryPoints.add(it) }
        }

        if (deliveryPoints.isNotEmpty()) {
            Log.d(TAG, "Pontos capturados: ${deliveryPoints.size}")
            // TODO: Enviar para MainActivity
        }
    }

    private fun findMarkerNodes(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val markers = mutableListOf<AccessibilityNodeInfo>()

        // Procura por nodes que contenham o ID de marcador
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // Verifica se é um marcador pelo ID
            val viewId = node.viewIdResourceName
            if (viewId?.contains(MARKER_ID_PREFIX) == true) {
                markers.add(node)
            }

            // Adiciona filhos à fila
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return markers
    }

    private fun extractMarkerInfo(node: AccessibilityNodeInfo): DeliveryPoint? {
        try {
            // Extrai o número do marcador do ID
            val markerId = node.viewIdResourceName ?: return null
            val markerNumber = markerId.substringAfterLast(MARKER_ID_PREFIX).toIntOrNull() ?: return null

            // Encontra as coordenadas do marcador
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)

            // Converte coordenadas da tela para coordenadas geográficas
            // Isso é uma simplificação, precisamos ajustar para coordenadas reais
            val latitude = bounds.exactCenterX().toDouble()
            val longitude = bounds.exactCenterY().toDouble()

            return DeliveryPoint(
                number = markerNumber,
                latitude = latitude,
                longitude = longitude
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao extrair informações do marcador", e)
            return null
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Serviço de acessibilidade interrompido")
    }

    data class DeliveryPoint(
        val number: Int,
        val latitude: Double,
        val longitude: Double
    )
}