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
  description = "Optionaler oeffentlicher SSH-Schluessel (leer = kein SSH, Zugriff nur ueber SSM)"
  type        = string
  default     = ""
}

variable "allowed_ssh_cidr" {
  description = "CIDR-Block fuer eingehenden SSH-Zugriff auf Port 22 (in Produktion auf eigene IP/32 einschraenken)"
  type        = string
  default     = "0.0.0.0/0"
}

variable "instance_profile_name" {
  description = "Vorhandenes Instance Profile (Learner Lab: LabInstanceProfile, eigene IAM-Rollen sind nicht erlaubt)"
  type        = string
  default     = "LabInstanceProfile"
}

variable "java_version" {
  description = "Major-Version von Amazon Corretto (mindestens 25)"
  type        = number
  default     = 25

  validation {
    condition     = var.java_version >= 25
    error_message = "Mindestens Java 25 (Amazon Corretto) ist gefordert."
  }
}

variable "ttyd_version" {
  description = "Feste ttyd-Version (Web-Konsole)"
  type        = string
  default     = "1.7.7"
}

variable "ttyd_sha256" {
  description = "SHA-256 von ttyd.x86_64 der gewaehlten Version (aus SHA256SUMS des GitHub-Releases)"
  type        = string
  default     = "8a217c968aba172e0dbf3f34447218dc015bc4d5e59bf51db2f2cd12b7be4f55"
}

variable "max_clients" {
  description = "Max. gleichzeitige Partien (= JVMs) auf der Instanz"
  type        = number
  default     = 8
}
