job "pscload" {
  datacenters = ["${datacenter}"]
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

    constraint {
      attribute = "$\u007Bnode.class\u007D"
      value     = "data"
    }

    update {
      max_parallel = 1
      min_healthy_time = "30s"
      progress_deadline = "5m"
      healthy_deadline = "2m"
    }

    network {
      port "http" {
        to = 8080
      }
    }

    task "pscload" {
      driver = "docker"
      config {
        image = "${artifact.image}:${artifact.tag}"
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
        destination = "local/file.env"
        env = true
        data = <<EOH
PUBLIC_HOSTNAME={{ with secret "psc-ecosystem/pscload" }}{{ .Data.data.public_hostname }}{{ end }}
JAVA_TOOL_OPTIONS="-Xms10g -Xmx10g -XX:+UseG1GC -Dspring.config.location=/secrets/application.properties -Dhttps.proxyHost=${proxy_host} -Dhttps.proxyPort=${proxy_port} -Dhttps.nonProxyHosts=${non_proxy_hosts}"
EOH
      }
      template {
        data = <<EOF
server.servlet.context-path=/pscload/v1
api.base.url=http://{{ range service "psc-api-maj" }}{{ .Address }}:{{ .Port }}{{ end }}/api
pscextract.base.url=http://{{ range service "pscextract" }}{{ .Address }}:{{ .Port }}{{ end }}/pscextract/v1
queue.name=file.upload
files.directory=/app/files-repo
cert.path=/secrets/certificate.pem
key.path=/secrets/key.pem
ca.path=/secrets/cacerts.pem
spring.rabbitmq.host={{ range service "psc-rabbitmq" }}{{ .Address }}{{ end }}
spring.rabbitmq.port={{ range service "psc-rabbitmq" }}{{ .Port }}{{ end }}
spring.rabbitmq.username={{ with secret "psc-ecosystem/rabbitmq" }}{{ .Data.data.user }}{{ end }}
spring.rabbitmq.password={{ with secret "psc-ecosystem/rabbitmq" }}{{ .Data.data.password }}{{ end }}
extract.download.url={{ with secret "psc-ecosystem/pscload" }}{{ .Data.data.extract_download_url }}{{ end }}
test.download.url={{ with secret "psc-ecosystem/pscload" }}{{ .Data.data.test_download_url }}{{ end }}
use.ssl=true
enable.scheduler={{ with secret "psc-ecosystem/pscload" }}{{ .Data.data.enable_scheduler }}{{ end }}
auto.continue.scheduler=false
schedule.cron.expression = 0 0 4/6 * * ?
schedule.cron.timeZone = Europe/Paris
management.endpoints.web.exposure.include=health,info,prometheus,metric
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
deactivation.excluded.profession.codes={{ with secret "psc-ecosystem/pscload" }}{{ .Data.data.deactivation_codes_exclusion_list }}{{ end }}
EOF
        destination = "secrets/application.properties"
        change_mode = "restart"
      }
      resources {
        cpu = 7000
        memory = 11264
      }
      service {
	    name = "$\u007BNOMAD_JOB_NAME\u007D"
        tags = ["urlprefix-$\u007BPUBLIC_HOSTNAME\u007D/pscload/v1/"]
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

    task "log-shipper" {
      driver = "docker"
      restart {
        interval = "30m"
        attempts = 5
        delay    = "15s"
        mode     = "delay"
      }
      meta {
        INSTANCE = "$\u007BNOMAD_ALLOC_NAME\u007D"
      }
      template {
        data = <<EOH
LOGSTASH_HOST = {{ range service "logstash" }}{{ .Address }}:{{ .Port }}{{ end }}
ENVIRONMENT = "${datacenter}"
EOH
        destination = "local/file.env"
        env = true
      }
      config {
        image = "prosanteconnect/filebeat:7.14.2"
      }
    }
  }
}

