package fr.ans.psc.pscload.service.emailing;

public enum EmailNature {
    PROCESS_FINISHED("Le process pscload s'est terminé, le fichier %s a été généré à partir du fichier %s.", "PSCLOAD - Fin de process"),
    PROCESS_RELAUNCHED("Le fichier %s n'est pas cohérent avec le fichier %s. Reprise du process.", "PSCLOAD - Reprise du process");

    public String message;
    public String subject;

    EmailNature(String message, String subject) {this.message = message; this.subject = subject;}
}
