/*
 * Plataforma de Banca Digital — modelo C4 (Structurizr DSL)
 *
 * Fuente autoritativa de los diagramas de arquitectura.
 *
 * Estado: Fase 2 — C4 Nivel 1 (Contexto del sistema).
 *   - Vista "ContextoPrincipal": alcance confirmado por el reto y supuestos aceptados.
 *   - Vista "ContextoAmpliado": agrega actores y sistemas candidatos y relaciones por validar.
 *   Los límites de confianza NO se representan aquí. Están en docs/14-limites-de-confianza.md,
 *   porque un límite de sistema de software (C4) no es un límite de confianza de red.
 *
 * Convención de confianza de las relaciones (propiedad "Confianza" y etiqueta):
 *   - Confirmada:   la establece el enunciado del reto.
 *   - Supuesta:     supuesto arquitectónico aceptado (docs/02-supuestos.md).
 *   - Por validar:  candidata. Solo aparece en la vista ampliada.
 *   - Ruta por validar: la dependencia la establece el reto,
 *     pero no la conexión directa (REL-05; remediación ARG01-M07).
 *
 * Decisiones vigentes que el modelo respeta:
 *   - ADR-0001: integración con el Core mediante ACL por dominio (detalle interno, no visible en el nivel 1).
 *   - ADR-0002: el API Gateway es un elemento interno; no aparece en el nivel 1.
 *   - ADR-0009: el Core Bancario es autoritativo para la identidad del cliente.
 *   - Supuesto S12: la plataforma no accede a la base de datos principal; esta no se modela.
 */
workspace "Plataforma de Banca Digital" "Arquitectura de la plataforma de banca digital web y móvil (reto técnico de Arquitecto de Soluciones)." {

    !identifiers hierarchical

    model {
        properties {
            "fase" "2"
        }

        // ================================================================
        // Actores
        // ================================================================
        cliente = person "Cliente bancario" "Cliente de la institución (socio, en el contexto cooperativo) que usa los canales web y móvil para consultar su información, cuentas y movimientos y realizar transferencias." {
            tags "Actor confirmado"
            properties {
                "Confianza" "Confirmada"
                "Fuente" "Reto: contexto, R1, R4, R6"
            }
        }
        solicitante = person "Solicitante de vinculación" "Persona que inicia su vinculación como nuevo cliente desde la app móvil, con verificación de identidad y reconocimiento facial." {
            tags "Actor confirmado"
            properties {
                "Confianza" "Confirmada"
                "Fuente" "Reto: R3"
            }
        }
        operadorSoporte = person "Operador de soporte" "Candidato. Personal de la institución que atiende casos de operaciones en verificación, conciliación y revisión manual de vinculaciones." {
            tags "Candidato"
            properties {
                "Confianza" "Por validar"
                "Fuente" "Derivado de ADR-0005, ADR-0006 y ADR-0009. No lo menciona el reto"
            }
        }
        auditor = person "Responsable de auditoría y cumplimiento" "Candidato. Personal que consulta el registro de auditoría de las acciones del cliente." {
            tags "Candidato"
            properties {
                "Confianza" "Por validar"
                "Fuente" "Derivado de R5. El reto no indica quién consulta la auditoría ni por qué medio"
            }
        }

        // ================================================================
        // Sistema en diseño
        // ================================================================
        plataforma = softwareSystem "Plataforma de Banca Digital" "Plataforma de canales digitales web y móvil que permite a clientes de una institución financiera consultar información, visualizar movimientos, realizar transferencias y completar procesos de vinculación digital, mediante integraciones seguras con sistemas bancarios y proveedores externos." {
            tags "Sistema en diseño"
            // El detalle interno (aplicaciones, API Gateway, servicios, ACL, caché,
            // auditoría, estado transaccional) se modela en la Fase 3 — Contenedores.
        }

        // ================================================================
        // Sistemas existentes de la institución
        // ================================================================
        core = softwareSystem "Core Bancario" "Sistema existente y autoritativo para los datos maestros del cliente, productos, cuentas, movimientos y transacciones bancarias (S1, S2, ADR-0009)." {
            tags "Sistema existente"
            properties {
                "Confianza" "Confirmada"
                "Fuente" "Reto: contexto"
            }
        }
        complementario = softwareSystem "Sistema Complementario de Clientes" "Sistema existente con información extendida del cliente. No es autoritativo para la creación ni para la identidad del cliente (S5, ADR-0009)." {
            tags "Sistema existente"
            properties {
                "Confianza" "Confirmada"
                "Fuente" "Reto: contexto"
            }
        }

        // ================================================================
        // Infraestructura financiera externa
        // ================================================================
        transferencias = softwareSystem "Sistema Externo de Transferencias" "Capacidad externa que procesa las transferencias interbancarias. La topología de integración (directa, mediante la infraestructura de pagos existente de la institución o mediante un intermediario autorizado) está por validar (S13, DD-16)." {
            tags "Infraestructura financiera externa"
            properties {
                "Confianza" "Confirmada (dependencia); topología por validar"
                "Fuente" "Reto: R1 («invoca servicios externos según el tipo»)"
            }
        }

        // ================================================================
        // Proveedores terceros
        // ================================================================
        kyc = softwareSystem "Proveedor KYC/Biométrico" "Capacidad externa de verificación de identidad: reconocimiento facial, prueba de vida, validación de documentos de identidad y resultado de la verificación. Proveedor no seleccionado (DD-14)." {
            tags "Proveedor tercero"
            properties {
                "Confianza" "Supuesta (S16)"
                "Fuente" "Reto: R2, R3"
            }
        }
        notificaciones = softwareSystem "Proveedores de Notificaciones" "Capacidades externas de entrega de notificaciones por notificación push, SMS y correo electrónico. Proveedores no seleccionados (DD-12)." {
            tags "Proveedor tercero"
            properties {
                "Confianza" "Supuesta (S17)"
                "Fuente" "Reto: R4 («propios o externos»)"
            }
        }

        // ================================================================
        // Sistemas candidatos (solo en la vista ampliada)
        // ================================================================
        idp = softwareSystem "Proveedor de Identidad (IdP)" "Candidato. Servidor de autorización OAuth 2.0 y OpenID Connect para clientes. Frontera por decidir: servicio externo (por ejemplo, SaaS) o parte de la Plataforma de Banca Digital (S9, DD-03)." {
            tags "Candidato"
            properties {
                "Confianza" "Por validar"
                "Fuente" "Reto: R2. Clasificación de frontera diferida"
            }
        }
        fuenteIdentidad = softwareSystem "Fuente Oficial de Identidad" "Candidato. Fuente oficial de datos de identificación de personas. Su uso requiere validar la autorización legal, la disponibilidad técnica y el mecanismo de acceso (DD-17)." {
            tags "Candidato"
            properties {
                "Confianza" "Por validar"
                "Fuente" "No lo menciona el reto. Derivado de R3 y de prácticas de KYC"
            }
        }

        // ================================================================
        // Relaciones — alcance principal (Confirmada / Supuesta)
        // ================================================================
        cliente -> plataforma "Consulta su información, cuentas y movimientos, y realiza transferencias propias e interbancarias" "" "Confirmada" {
            properties {
                "ID" "REL-01"
                "Confianza" "Confirmada"
            }
        }
        solicitante -> plataforma "Inicia su vinculación digital y completa la verificación de identidad y biométrica desde la app móvil" "" "Confirmada" {
            properties {
                "ID" "REL-02"
                "Confianza" "Confirmada"
            }
        }
        plataforma -> core "Consulta datos del cliente, cuentas y movimientos, y solicita la ejecución de transferencias y la creación de clientes" "" "Confirmada" {
            properties {
                "ID" "REL-03"
                "Confianza" "Confirmada (consultas); decisión ADR-0009 (creación de clientes)"
            }
        }
        plataforma -> complementario "Consulta información extendida del cliente" "" "Confirmada" {
            properties {
                "ID" "REL-04"
                "Confianza" "Confirmada"
            }
        }
        plataforma -> transferencias "Solicita el procesamiento de transferencias interbancarias y consulta su resultado (dependencia lógica; ruta por validar)" "" "Ruta por validar" {
            properties {
                "ID" "REL-05"
                "Confianza" "Confirmada (dependencia); topología por validar"
            }
        }
        plataforma -> kyc "Verifica la identidad del solicitante mediante reconocimiento facial, prueba de vida y validación de documento" "" "Supuesta" {
            properties {
                "ID" "REL-06"
                "Confianza" "Supuesta (S16)"
            }
        }
        plataforma -> notificaciones "Solicita el envío de notificaciones de movimientos y del estado de la vinculación" "" "Supuesta" {
            properties {
                "ID" "REL-07"
                "Confianza" "Supuesta (S17)"
            }
        }
        notificaciones -> cliente "Entrega notificaciones de cada movimiento" "" "Supuesta" {
            properties {
                "ID" "REL-08"
                "Confianza" "Supuesta (S17)"
            }
        }
        notificaciones -> solicitante "Entrega notificaciones del estado de la vinculación" "" "Supuesta" {
            properties {
                "ID" "REL-09"
                "Confianza" "Supuesta (S17)"
            }
        }

        // ================================================================
        // Relaciones — por validar (solo vista ampliada)
        // ================================================================
        cliente -> idp "Se autentica para acceder a los canales digitales (si el IdP es externo)" "" "Por validar" {
            properties {
                "ID" "REL-10"
                "Confianza" "Por validar (DD-03)"
            }
        }
        plataforma -> idp "Delega la autenticación del cliente y la emisión de tokens OAuth 2.0 (si el IdP es externo)" "" "Por validar" {
            properties {
                "ID" "REL-11"
                "Confianza" "Por validar (DD-03)"
            }
        }
        plataforma -> fuenteIdentidad "Contrasta los datos de identificación del solicitante (mecanismo de acceso por validar)" "" "Por validar" {
            properties {
                "ID" "REL-12"
                "Confianza" "Por validar (DD-17)"
            }
        }
        core -> plataforma "Informa movimientos originados en otros canales para su notificación" "" "Por validar" {
            properties {
                "ID" "REL-13"
                "Confianza" "Por validar (Q-04)"
            }
        }
        operadorSoporte -> plataforma "Atiende operaciones en verificación, conciliaciones y vinculaciones en revisión manual" "" "Por validar" {
            properties {
                "ID" "REL-14"
                "Confianza" "Por validar (DD-18)"
            }
        }
        auditor -> plataforma "Consulta el registro de auditoría de las acciones del cliente" "" "Por validar" {
            properties {
                "ID" "REL-15"
                "Confianza" "Por validar (DD-18)"
            }
        }
    }

    views {
        systemContext plataforma "ContextoPrincipal" "C4 Nivel 1 — Contexto principal: alcance confirmado por el reto y supuestos aceptados." {
            title "C4 Nivel 1 — Contexto principal de la Plataforma de Banca Digital"
            include *
            exclude "element.tag==Candidato"
            exclude "relationship.tag==Por validar"
            autolayout lr 300 200
        }

        systemContext plataforma "ContextoAmpliado" "C4 Nivel 1 — Contexto ampliado: agrega actores, sistemas y relaciones candidatos que requieren validación (líneas punteadas)." {
            title "C4 Nivel 1 — Contexto ampliado de la Plataforma de Banca Digital (incluye candidatos por validar)"
            include *
            autolayout lr 300 200
        }

        // TODO (Fase 3): container plataforma "Contenedores" { ... }
        // TODO (Fase 4): component <contenedor> "Componentes-<contenedor>" { ... }
        // TODO (Fase de infraestructura): deployment plataforma "Producción" "Despliegue" { ... }

        terminology {
            person "Persona"
            softwareSystem "Sistema de software"
            relationship "Relación"
        }

        styles {
            element "Element" {
                fontSize 22
            }
            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }
            element "Sistema en diseño" {
                background #1168bd
                color #ffffff
            }
            element "Sistema existente" {
                background #5b6770
                color #ffffff
            }
            element "Infraestructura financiera externa" {
                background #7a5c2e
                color #ffffff
            }
            element "Proveedor tercero" {
                background #8a8a8a
                color #ffffff
            }
            element "Candidato" {
                background #ffffff
                color #5b6770
                stroke #5b6770
                border dashed
            }
            relationship "Relationship" {
                fontSize 20
            }
            relationship "Confirmada" {
                style solid
            }
            relationship "Supuesta" {
                style dashed
            }
            relationship "Ruta por validar" {
                style dashed
                color #7a5c2e
                thickness 3
            }
            relationship "Por validar" {
                style dotted
                color #a0a0a0
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
