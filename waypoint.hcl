project = "prosanteconnect/pscload"

# Labels can be specified for organizational purposes.
labels = { "domaine" = "psc" }

runner {
  enabled = true
  data_source "git" {
    url = "https://github.com/prosanteconnect/pscload.git"
    ref = var.datacenter
  }
  poll {
    enabled = true
  }
}

# An application to deploy.
app "prosanteconnect/pscload" {

  # Build specifies how an application should be deployed.
  build {
    use "pack" {
      builder = "registry.repo.proxy-dev-forge.asip.hst.fluxus.net/heroku/buildpacks:18"
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
  default = "dc1"
}
