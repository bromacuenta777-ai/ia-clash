# RoyaleAI — Agente Táctico Autónomo para Clash Royale (Sin PC)

Aplicación Android escrita en **Kotlin + Jetpack Compose** diseñada para funcionar directamente en el teléfono sin PC ni root:

- **Control Táctil Nativo (`AccessibilityService`)**: Ejecuta toques y arrastres reales sobre la pantalla de Clash Royale e incluye un **HUD flotante** para iniciar/pausar en vivo.
- **Visión de Arena en Vivo (`MediaProjection`)**: Lee barra de elixir, disponibilidad de cartas, amenazas por línea y estado de partida en tiempo real.
- **Cerebro Táctico Pro + Gemini 3.8 Flash**: Combina colocación rápida en cuadrícula `18×32` (defensa `4-3`, *kite* central, castigo en línea contraria) con análisis multimodal de **Firebase AI Logic**.
- **Calibración y Laboratorio IA**: Ajuste proporcional de pantalla (`20:9`, `19.5:9`, `16:9`), analizador de capturas y simulador táctico `18×32`.

## Descargar el APK desde GitHub
Al subir este repositorio a GitHub, el flujo automático de **GitHub Actions** compila y publica `RoyaleAI.apk` en la sección **Releases** de tu repositorio listo para descargar e instalar en Android.
