variable "aws_region" {
  description = "AWS-Region fuer die Infrastruktur (z. B. us-east-1 im AWS Learner Lab oder eu-central-1)"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Zielumgebung der Infrastruktur (dev, staging oder prod)"
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Die Umgebung muss 'dev', 'staging' oder 'prod' sein."
  }
}

variable "project_name" {
  description = "Projektname fuer Benennung und Tagging der AWS-Ressourcen"
  type        = string
  default     = "tictactest"
}

variable "instance_type" {
  description = "EC2-Instanztyp (aus Kostengruenden auf kostenguenstige Micro/Small-Instanzen begrenzt)"
  type        = string
  default     = "t2.micro"

  validation {
    condition     = contains(["t2.micro", "t3.micro", "t2.small", "t3.small"], var.instance_type)
    error_message = "Aus Kostenkontrollgruenden sind nur t2.micro, t3.micro, t2.small oder t3.small erlaubt."
  }
}

variable "ami_id" {
  description = "Optionale AMI-ID; falls leer, wird automatisch das neueste Amazon Linux 2023 AMI verwendet"
  type        = string
  default     = ""
}

variable "ssh_public_key" {
  description = "Oeffentlicher SSH-Schluessel fuer den Zugriff auf die EC2-Instanz (lokal erzeugt, in terraform.tfvars oder per TF_VAR_ssh_public_key setzen)"
  type        = string
  default     = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIExamplePublicKeyReplaceInTfvars tictactest-key"
}

variable "allowed_ssh_cidr" {
  description = "CIDR-Block fuer eingehenden SSH-Zugriff auf Port 22 (in Produktion auf eigene IP/32 einschraenken)"
  type        = string
  default     = "0.0.0.0/0"
}
