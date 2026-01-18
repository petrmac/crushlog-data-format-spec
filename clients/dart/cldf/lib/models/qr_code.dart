import 'package:json_annotation/json_annotation.dart';

part 'qr_code.g.dart';

/// QR code data for physical route marking (v1.3.0+)
@JsonSerializable(includeIfNull: false)
class QrCode {
  /// Creates a new [QrCode] instance
  QrCode({
    this.data,
    this.url,
    this.ipfsHash,
    this.blockchainTx,
    this.generatedAt,
  });

  /// Creates a [QrCode] from JSON
  factory QrCode.fromJson(Map<String, dynamic> json) => _$QrCodeFromJson(json);

  /// Generated QR code data string
  final String? data;

  /// Public URL for the route
  final String? url;

  /// IPFS hash for CLDF archive reference
  final String? ipfsHash;

  /// Blockchain transaction hash for permanent record
  final String? blockchainTx;

  /// When the QR code was generated
  final DateTime? generatedAt;

  /// Converts this [QrCode] to JSON
  Map<String, dynamic> toJson() => _$QrCodeToJson(this);
}
