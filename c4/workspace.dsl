/*
 * Plataforma de Banca Digital — modelo C4 (Structurizr DSL)
 *
 * Estado: Fase 0–1. Solo se declaran los elementos que el enunciado del reto
 * nombra explícitamente. Las relaciones, los contenedores, los componentes,
 * el despliegue y las vistas se diseñan en las fases siguientes.
 *
 * Decisiones vigentes que el modelo debe respetar:
 *   - ADR-0001: integración con el Core mediante ACL por dominio.
 *   - ADR-0002: el API Gateway es una frontera de tráfico y fachada,
 *               no un orquestador de negocio.
 *   - ADR-0009: el Core Bancario es autoritativo para la identidad del cliente.
 *   - Supuesto S12: la plataforma no accede directamente a la base de datos principal.
 */
workspace "Plataforma de Banca Digital" "Arquitectura de la plataforma de banca digital web y móvil (reto técnico de Arquitecto de Soluciones)." {

    !identifiers hierarchical

    model {
        properties {
            "fase" "0-1"
        }

        // --- Actores (enunciado del reto) ---------------------------------
        cliente = person "Cliente" "Cliente de la institución que usa la banca digital web y móvil para consultar movimientos y realizar transferencias. En el contexto cooperativo, socio."
        solicitante = person "Solicitante de onboarding" "Persona que se registra como nuevo cliente desde la app móvil con reconocimiento facial."

        // --- Sistema en alcance --------------------------------------------
        plataforma = softwareSystem "Plataforma de Banca Digital" "Permite consultar datos del cliente y movimientos, realizar transferencias propias e interbancarias y gestionar el onboarding digital." {
            tags "En alcance"

            // TODO (Fase 3 — Contenedores): SPA web, app móvil, API Gateway,
            // servicios de dominio (clientes, movimientos, transferencias, onboarding),
            // ACL por dominio, caché, base de datos de auditoría, almacén de estado transaccional.
            // No se anticipan microservicios antes de esa fase.
        }

        // --- Sistemas externos (enunciado del reto) -------------------------
        core = softwareSystem "Core Bancario" "Sistema existente y autoritativo para datos básicos, productos, cuentas y movimientos (S1, S2)." {
            tags "Externo"
        }
        complementario = softwareSystem "Sistema complementario de datos del cliente" "Sistema existente con los datos extendidos del cliente (S5)." {
            tags "Externo"
        }

        // TODO (Fase 2 — Contexto): confirmar y modelar los sistemas externos
        // adicionales: proveedor de identidad (S9, DD-03), proveedores de KYC y
        // biometría (DD-14), proveedores de notificación (DD-12) y sistema de pagos
        // interbancarios del BCE (S13), así como todas las relaciones.
        // La base de datos principal se considera interna a los sistemas existentes (S12).
    }

    views {
        // TODO (Fase 2): systemContext plataforma "Contexto" { ... }
        // TODO (Fase 3): container plataforma "Contenedores" { ... }
        // TODO (Fase 4): component <contenedor> "Componentes-<contenedor>" { ... }
        // TODO (Fase de infraestructura): deployment plataforma "Producción" "Despliegue" { ... }

        styles {
            element "Person" {
                shape person
            }
            element "Externo" {
                background #999999
                color #ffffff
            }
            element "En alcance" {
                background #1168bd
                color #ffffff
            }
        }
    }
}
