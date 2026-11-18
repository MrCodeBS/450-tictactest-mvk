output "instance_id" {
  description = "ID der erstellten EC2-Instanz"
  value       = aws_instance.tictactest_server.id
}

output "public_ip" {
  description = "Oeffentliche IP-Adresse der EC2-Instanz"
  value       = aws_instance.tictactest_server.public_ip
}

output "public_dns" {
  description = "Oeffentlicher DNS-Name der EC2-Instanz"
  value       = aws_instance.tictactest_server.public_dns
}

output "ssh_connection_command" {
  description = "Beispielbefehl fuer die SSH-Verbindung zur EC2-Instanz"
  value       = "ssh -i ~/.ssh/tictactest_key ec2-user@${aws_instance.tictactest_server.public_ip}"
}
