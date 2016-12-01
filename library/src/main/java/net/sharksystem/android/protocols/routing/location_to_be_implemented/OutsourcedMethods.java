package net.sharksystem.android.protocols.routing.location_to_be_implemented;

public class OutsourcedMethods {

//    private boolean isMovementProfileCloser(SpatialSemanticTag spatialSemanticTag) throws SharkKBException, ParseException {
//        SpatialAlgebra algebra = new SpatialAlgebra();
//        String wkt = spatialSemanticTag.getGeometry().getWKT();
//        String ewkt = spatialSemanticTag.getGeometry().getEWKT();
//
//        if (!TextUtils.isEmpty(wkt) && algebra.isValidWKT(wkt)) {
//            Point destinationPoint = new WKTReader().read(wkt).getCentroid();
//            Location destination = new Location("");
//            destination.setLatitude(destinationPoint.getX());
//            destination.setLongitude(destinationPoint.getY());
//
//            Point movementProfileCentroidPoint = mCoordinateContentProvider.getConvexHull().getCentroid();
//            Location movementProfileCentroid = new Location("");
//            movementProfileCentroid.setLatitude(movementProfileCentroidPoint.getX());
//            movementProfileCentroid.setLongitude(movementProfileCentroidPoint.getY());
//
//            Location lastLocation = LocationReceiver.getLastLocation();
//
//            return (movementProfileCentroid.distanceTo(destination) < lastLocation.distanceTo(destination));
//        } else if (!TextUtils.isEmpty(ewkt) && algebra.isValidEWKT(ewkt)) {
//            // TODO there's no EWKT reader...mb also works with WKTREADER?
//            return false;
//        } else {
//            // TODO throw exception cause of no valid wkt?
//            return false;
//        }
//    }
//
//    private boolean isTimeSpanInPast(TimeSemanticTag time) {
//        return (time.getFrom() + time.getDuration()) < System.currentTimeMillis();
//    }
//
//    private void checkMessagesToRoute() {
//        List<MessageDTO> messages = mMessageContentProvider.getAllMessages();
//        for (int i = messages.size() - 1; i >= 0; i--) {
//            MessageDTO message = messages.get(i);
//            if (message.getReceiverPeer() != null) {
//                this.checkReceiverPeer(message);
//            } else if (message.getReceiverSpatial() != null) {
//                this.checkReceiverSpatial(message);
//            } else if (message.getReceiverTime() != null) {
//                this.checkReceiverTime(message);
//            }
//        }
//    }
//
//    private void checkReceiverPeer(MessageDTO message) {
//        List<PeerSemanticTag> nearbyPeers = mEngine.getNearbyPeers();
//        PeerSemanticTag receiver = message.getReceiverPeer();
//
//        for (PeerSemanticTag peer : nearbyPeers) {
//            if (peer.identical(receiver)) {
//                mEngine.sendMessage(message, message.getReceiverPeer().getAddresses());
//                mMessageContentProvider.delete(message);
//                return;
//            }
//        }
//
//        //Receiver is not nearby, so try to send it to as many new ppl as possible
//        this.forwardMessage(message);
//    }
//
//    private void checkReceiverSpatial(MessageDTO message) {
//        try {
//            Geometry geometry = new WKTReader().read(message.getReceiverSpatial().getGeometry().getWKT());
//            Point destinationPoint = geometry.getCentroid();
//            Location destination = new Location("");
//            destination.setLatitude(destinationPoint.getX());
//            destination.setLongitude(destinationPoint.getY());
//
//            if (destination.distanceTo(LocationReceiver.getLastLocation()) < 100) {
//                this.forwardMessage(message);
//            }
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void checkReceiverTime(MessageDTO message) {
//        long currentTime = System.currentTimeMillis();
//        if (currentTime >= message.getReceiverTime().getFrom() && currentTime <= message.getReceiverTime().getFrom() + message.getReceiverTime().getDuration()) {
//            this.forwardMessage(message);
//        }
//    }
}
