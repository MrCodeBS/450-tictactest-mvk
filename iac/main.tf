data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
}

# Learner Lab: eigene IAM-Rollen sind verboten -> vorhandenes Instance Profile (LabRole) verwenden.
# Darueber greift die Instanz ohne Access Keys auf S3 und SSM zu.
data "aws_iam_instance_profile" "lab" {
  name = var.instance_profile_name
}

locals {
  name = "${var.project_name}-${var.environment}"
}

# ---------------------------------------------------------------------------
# Optionaler SSH-Zugang (Deployment laeuft ueber SSM, SSH nur fuers Debugging)
# ---------------------------------------------------------------------------
resource "aws_key_pair" "deployer" {
  count      = var.ssh_public_key != "" ? 1 : 0
  key_name   = "${local.name}-key"
  public_key = var.ssh_public_key
}

# ---------------------------------------------------------------------------
# Netzwerk
# ---------------------------------------------------------------------------
resource "aws_security_group" "tictactest_sg" {
  name        = "${local.name}-sg"
  description = "TicTacTest: HTTP fuer Web-Konsole, optional SSH"

  tags = {
    Name = "${local.name}-sg"
  }
}

# Port 80: nginx -> Web-Konsole (ttyd) und Versionsabfrage. Muss oeffentlich sein, sonst kann niemand spielen.
resource "aws_vpc_security_group_ingress_rule" "http" {
  security_group_id = aws_security_group.tictactest_sg.id
  description       = "HTTP Web-Konsole und /version.txt"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  cidr_ipv4         = "0.0.0.0/0"
}

# Port 22: nur wenn ein SSH-Key gesetzt ist, und nur vom erlaubten CIDR.
resource "aws_vpc_security_group_ingress_rule" "ssh" {
  count             = var.ssh_public_key != "" ? 1 : 0
  security_group_id = aws_security_group.tictactest_sg.id
  description       = "SSH (Debugging)"
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
  cidr_ipv4         = var.allowed_ssh_cidr
}

# Ausgehend: dnf, GitHub (ttyd-Download), S3 und SSM-Endpunkte (HTTPS) + HTTP fuer Paket-Mirrors.
resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.tictactest_sg.id
  description       = "Ausgehend (Pakete, S3, SSM)"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

# ---------------------------------------------------------------------------
# Instanz
# ---------------------------------------------------------------------------
resource "aws_instance" "tictactest_server" {
  ami                    = var.ami_id != "" ? var.ami_id : data.aws_ami.amazon_linux.id
  instance_type          = var.instance_type
  key_name               = var.ssh_public_key != "" ? aws_key_pair.deployer[0].key_name : null
  vpc_security_group_ids = [aws_security_group.tictactest_sg.id]
  iam_instance_profile   = data.aws_iam_instance_profile.lab.name

  user_data = templatefile("${path.module}/user_data.yaml.tftpl", {
    aws_region       = var.aws_region
    artifacts_bucket = local.artifacts_bucket
    max_clients      = var.max_clients
    java_version     = var.java_version
    ttyd_version     = var.ttyd_version
    ttyd_sha256      = var.ttyd_sha256
    service_unit     = filebase64("${path.module}/files/tictactest.service")
    nginx_conf       = filebase64("${path.module}/files/nginx.conf")
    game_script      = filebase64("${path.module}/files/tictactest-game.sh")
    prepare_script   = filebase64("${path.module}/files/tictactest-prepare.sh")
    deploy_script    = filebase64("${path.module}/files/tictactest-deploy.sh")
  })

  # Bewusst false: eine Aenderung am Setup ersetzt die Instanz NICHT (EIP + laufende Version bleiben).
  # user_data laeuft aber nur beim allerersten Boot - Setup-Aenderungen erfordern daher ein
  # bewusstes "tofu apply -replace=aws_instance.tictactest_server" + erneutes Deployment.
  user_data_replace_on_change = false

  metadata_options {
    http_tokens = "required" # IMDSv2
  }

  root_block_device {
    volume_size = 8
    volume_type = "gp3"
  }

  tags = {
    Name = "${local.name}-server"
  }
}

# Feste oeffentliche Adresse, die neue Learner-Lab-Sessions (Stop/Start) ueberlebt.
resource "aws_eip" "tictactest" {
  domain   = "vpc"
  instance = aws_instance.tictactest_server.id

  tags = {
    Name = "${local.name}-eip"
  }
}
