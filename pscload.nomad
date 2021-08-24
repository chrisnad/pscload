job "pscload" {
  datacenters = ["dc1"]
  type = "service"
  vault {
    policies = ["psc-ecosystem"]
    change_mode = "restart"
  }

  group "pscload-services" {
    count = "1"
    restart {
      attempts = 3
      delay = "60s"
      interval = "1h"
      mode = "fail"
    }

    update {
      max_parallel      = 1
    }

    network {
      port "http" {
        to = 8080
      }
    }

    task "pscload" {
      env {
        JAVA_TOOL_OPTIONS="-Xms10g -Xmx10g -XX:+UseG1GC -Dspring.config.location=/secrets/application.properties"
      }
      driver = "docker"
      config {
        image = "prosanteconnect/pscload:latest"
        volumes = [
          "name=pscload-data,io_priority=high,size=3,repl=3:/app/files-repo"
        ]
        volume_driver = "pxd"
        ports = ["http"]
      }
      template {
        data = <<EOH
{{ with secret "psc-ecosystem/pscload" }}{{ .Data.data.certificate }}{{ end }}
EOH
        destination = "secrets/certificate.pem"
      }
      template {
        data = <<EOH
{{ with secret "psc-ecosystem/pscload" }}{{ .Data.data.private_key }}{{ end }}
EOH
        destination = "secrets/key.pem"
      }
      template {
        data = <<EOH
{{ with secret "psc-ecosystem/pscload" }}{{ .Data.data.cacerts }}{{ end }}
EOH
        destination = "secrets/cacerts.pem"
      }
      template {
        data = <<EOF
server.servlet.context-path=/pscload/v1
api.base.url=http://{{ range service "psc-api-maj" }}{{ .Address }}:{{ .Port }}{{ end }}/api
queue.name=file.upload
files.directory=/app/files-repo
cert.path=/secrets/certificate.pem
key.path=/secrets/key.pem
ca.path=/secrets/cacerts.pem
spring.rabbitmq.host={{ range service "psc-rabbitmq" }}{{ .Address }}{{ end }}
spring.rabbitmq.port={{ range service "psc-rabbitmq" }}{{ .Port }}{{ end }}
spring.rabbitmq.username={{ with secret "psc-ecosystem/rabbitmq" }}{{ .Data.data.user }}{{ end }}
spring.rabbitmq.password={{ with secret "psc-ecosystem/rabbitmq" }}{{ .Data.data.password }}{{ end }}
extract.download.url=https://service.annuaire.sante.fr/annuaire-sante-webservices/V300/services/extraction/Extraction_ProSanteConnect
test.download.url=https://raw.githubusercontent.com/vsorette/psc-file-repo/main/
use.ssl=true
enable.scheduler=false
schedule.cron.expression = 0 0 4/6 * * ?
schedule.cron.timeZone = Europe/Paris
management.endpoints.web.exposure.include=health,info,prometheus,metric
EOF
        destination = "secrets/application.properties"
      }
      resources {
        cpu = 7000
        memory = 11264
      }
      service {
        name = "${NOMAD_JOB_NAME}"
        tags = ["urlprefix-pscload.psc.api.esante.gouv.fr/pscload/v1/"]
        port = "http"
        check {
          type = "http"
          path = "/pscload/v1/check"
          port = "http"
          interval = "10s"
          timeout = "2s"
        }
      }
    }
  }
}

