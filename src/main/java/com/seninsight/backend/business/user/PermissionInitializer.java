package com.seninsight.backend.business.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class PermissionInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        seedPermissions();
    }

    private void seedPermissions() {
        List<PermDef> defs = List.of(
            // DEMANDES
            new PermDef("DEMANDE_CREATE",        "Créer une demande",               "Création de nouvelles demandes",                "DEMANDES"),
            new PermDef("DEMANDE_READ",           "Consulter les demandes",          "Voir la liste et le détail des demandes",       "DEMANDES"),
            new PermDef("DEMANDE_UPDATE",         "Modifier une demande",            "Modification des demandes en brouillon",        "DEMANDES"),
            new PermDef("DEMANDE_DELETE",         "Supprimer une demande",           "Suppression des demandes en brouillon",         "DEMANDES"),
            new PermDef("DEMANDE_SUBMIT",         "Soumettre une demande",           "Soumission officielle d'une demande",           "DEMANDES"),
            new PermDef("DEMANDE_ASSIGN",         "Assigner une demande",            "Assigner une demande à un instructeur",         "DEMANDES"),
            new PermDef("DEMANDE_REASSIGN",       "Réassigner une demande",          "Réassigner une demande à un autre instructeur", "DEMANDES"),
            new PermDef("DEMANDE_APPROVE",        "Approuver une demande",           "Approuver une demande d'agrément",              "DEMANDES"),
            new PermDef("DEMANDE_REJECT",         "Rejeter une demande",             "Rejeter une demande d'agrément",                "DEMANDES"),
            new PermDef("DEMANDE_INCOMPLETE",     "Signaler dossier incomplet",      "Signaler un dossier incomplet",                 "DEMANDES"),
            new PermDef("DEMANDE_VERIFY",         "Vérifier (inspecteur)",           "Étape de vérification inspecteur",              "DEMANDES"),
            new PermDef("DEMANDE_STATS",          "Statistiques demandes",           "Voir les statistiques globales des demandes",   "DEMANDES"),

            // DOCUMENTS
            new PermDef("DOCUMENT_UPLOAD",        "Uploader un document",            "Upload de documents justificatifs",             "DOCUMENTS"),
            new PermDef("DOCUMENT_DOWNLOAD",      "Télécharger un document",         "Téléchargement de documents",                   "DOCUMENTS"),
            new PermDef("DOCUMENT_DELETE",        "Supprimer un document",           "Suppression de documents",                      "DOCUMENTS"),
            new PermDef("DOCUMENT_VALIDATE",      "Valider un document",             "Validation d'un document par un instructeur",   "DOCUMENTS"),
            new PermDef("DOCUMENT_REJECT",        "Rejeter un document",             "Rejet d'un document par un instructeur",        "DOCUMENTS"),

            // UTILISATEURS
            new PermDef("USER_READ",              "Consulter les utilisateurs",      "Voir la liste des utilisateurs",                "UTILISATEURS"),
            new PermDef("USER_CREATE",            "Créer un utilisateur",            "Création de nouveaux utilisateurs",             "UTILISATEURS"),
            new PermDef("USER_UPDATE",            "Modifier un utilisateur",         "Modification des utilisateurs",                 "UTILISATEURS"),
            new PermDef("USER_DELETE",            "Désactiver un utilisateur",       "Désactivation d'un utilisateur",                "UTILISATEURS"),
            new PermDef("USER_ACTIVATE",          "Activer/Suspendre utilisateur",   "Activer ou suspendre un utilisateur",           "UTILISATEURS"),

            // ROLES & PERMISSIONS
            new PermDef("ROLE_READ",              "Consulter les rôles",             "Voir la liste des rôles",                       "ROLES"),
            new PermDef("ROLE_CREATE",            "Créer un rôle",                   "Création de nouveaux rôles",                    "ROLES"),
            new PermDef("ROLE_UPDATE",            "Modifier un rôle",                "Modification des rôles",                        "ROLES"),
            new PermDef("ROLE_DELETE",            "Supprimer un rôle",               "Suppression des rôles",                         "ROLES"),
            new PermDef("ROLE_PERMISSIONS",       "Gérer les permissions",           "Assigner des permissions aux rôles",            "ROLES"),

            // NOTIFICATIONS
            new PermDef("NOTIFICATION_READ",      "Consulter les notifications",     "Voir les notifications",                        "NOTIFICATIONS"),
            new PermDef("NOTIFICATION_SEND",      "Envoyer une notification",        "Envoi de notifications manuelles",              "NOTIFICATIONS"),

            // PAIEMENTS
            new PermDef("PAIEMENT_INIT",          "Initier un paiement",             "Initier une transaction de paiement",           "PAIEMENTS"),
            new PermDef("PAIEMENT_CONFIRM",       "Confirmer un paiement",           "Confirmer un paiement reçu",                    "PAIEMENTS"),
            new PermDef("PAIEMENT_READ",          "Consulter les paiements",         "Voir les informations de paiement",             "PAIEMENTS"),

            // TYPES D'AGREMENTS
            new PermDef("TYPE_AGREMENT_READ",     "Consulter les types",             "Voir les types d'agrément disponibles",         "TYPE_AGREMENT"),
            new PermDef("TYPE_AGREMENT_MANAGE",   "Gérer les types",                 "Créer et modifier les types d'agrément",        "TYPE_AGREMENT"),

            // SIGNATURES
            new PermDef("SIGNATURE_REQUEST",      "Demander une signature",          "Envoyer une demande de signature",              "SIGNATURES"),
            new PermDef("SIGNATURE_READ",         "Consulter les signatures",        "Voir les signatures d'une demande",             "SIGNATURES")
        );

        int created = 0;
        for (PermDef def : defs) {
            if (!permissionRepository.existsByCode(def.code())) {
                Permission p = Permission.builder()
                        .code(def.code())
                        .libelle(def.libelle())
                        .description(def.description())
                        .module(def.module())
                        .actif(true)
                        .build();
                permissionRepository.save(p);
                created++;
            }
        }
        if (created > 0) {
            log.info("Permissions initialisées : {} nouvelles permissions créées (total catalogue : {})",
                    created, permissionRepository.count());
        }
    }

    private record PermDef(String code, String libelle, String description, String module) {}
}
