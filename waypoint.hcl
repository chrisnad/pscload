project = "prosanteconnect/pscload"

# Labels can be specified for organizational purposes.
labels = { "domaine" = "psc" }

runner {
  enabled = true
  data_source "git" {
    url = "https://github.com/prosanteconnect/pscload.git"
    ref = "main"
  }
}

# An application to deploy.
app "prosanteconnect/pscload" {

  # Build specifies how an application should be deployed.
  build {
    use "pack" {
      builder = "gcr.io/buildpacks/builder:v1"
    }

    registry {
      use "docker" {
        image = artifact.image
        tag   = gitrefpretty()
        encoded_auth = filebase64("/secrets/dockerAuth.json")
      }
    }
  }

  # Deploy to Nomad
  deploy {
    use "nomad-jobspec" {
      jobspec = templatefile("${path.app}/pscload.nomad.tpl", {
        datacenter = var.datacenter
      })
    }
  }
}

variable "datacenter" {
  type    = string
  default = "production"
}
