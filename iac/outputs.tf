# --- Fuer Benutzer -----------------------------------------------------------
output "frontend_url" {
  description = "URL zum Spielen (S3 Static Website)"
  value       = "http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}"
}

output "console_url" {
  description = "URL der Web-Konsole (ttyd hinter nginx)"
  value       = "http://${aws_eip.tictactest.public_dns}/"
}

output "version_url" {
  description = "URL der Versionsabfrage des Backends"
  value       = "http://${aws_eip.tictactest.public_dns}/version.txt"
}

# --- Fuer die Pipeline (GitHub Variables) ------------------------------------
output "instance_id" {
  description = "ID der EC2-Instanz (Ziel fuer aws ssm send-command)"
  value       = aws_instance.tictactest_server.id
}

output "public_ip" {
  description = "Feste oeffentliche IP (Elastic IP)"
  value       = aws_eip.tictactest.public_ip
}

output "artifacts_bucket" {
  description = "Privater Bucket fuer Release-Jars"
  value       = local.artifacts_bucket
}

output "frontend_bucket" {
  description = "Oeffentlicher Bucket fuers Frontend"
  value       = local.frontend_bucket
}

output "aws_region" {
  description = "AWS-Region"
  value       = var.aws_region
}

output "github_variables" {
  description = "Werte zum Eintragen unter GitHub > Settings > Secrets and variables > Actions > Variables"
  value       = <<-EOT
    AWS_REGION=${var.aws_region}
    INSTANCE_ID=${aws_instance.tictactest_server.id}
    ARTIFACTS_BUCKET=${local.artifacts_bucket}
    FRONTEND_BUCKET=${local.frontend_bucket}
    FRONTEND_URL=http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}
    VERSION_URL=http://${aws_eip.tictactest.public_dns}/version.txt
  EOT
}

output "ssh_connection_command" {
  description = "SSH-Befehl (nur wenn ssh_public_key gesetzt ist)"
  value       = var.ssh_public_key != "" ? "ssh -i ~/.ssh/tictactest_key ec2-user@${aws_eip.tictactest.public_ip}" : "SSH deaktiviert - aws ssm start-session --target ${aws_instance.tictactest_server.id}"
}
